package phylogenetic;

import Jama.Matrix;
import graphlab.graph.graph.EdgeModel;
import graphlab.graph.graph.GraphModel;
import graphlab.graph.graph.GraphPoint;
import graphlab.graph.graph.SubGraph;
import graphlab.graph.graph.VertexModel;
import graphlab.library.BaseEdgeProperties;
import graphlab.library.BaseGraph;
import graphlab.library.BaseVertexProperties;
import graphlab.library.algorithms.Algorithm;
import graphlab.library.algorithms.util.EventUtils;
import graphlab.platform.StaticUtils;
import graphlab.platform.lang.Pair;
import graphlab.plugins.main.core.AlgorithmUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;
import java.util.Vector;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class TripNet extends Algorithm {
	static TripNet ths;
	Vector<Triplet> tow = new Vector<Triplet>();
	GraphModel Gf;
	static int lastXi = 0;
	HashMap<String, Integer> F0 = null;
	HashMap<Pair<Integer, Integer>, Integer> weights;

	// @Parameter(name = "Triplets File")
	public static File tripletsFile = new File(
			"C:\\Documents and Settings\\zinoo\\Desktop\\BioInf\\Triplets\\DataSetsl\\for azin email\\marlon40\\tripletmarlon40.txt");
	// @Parameter(name = "Integer Programming File")
	public static File integerProgrammingFile = new File(
			"D:\\GraphLab\\SVN\\head\\src\\graphlab\\extensions\\phylogenetic\\integer programming sample output.txt");
	static int algDepth = 0;
	static boolean verbose = false;
	int numOfLeaves;
	static GUI gui;
	static double TRIPLET_WEIGHT_MIN_BOUND = 0.4;

	public static int NORMAL_SPEED_NUM_OF_CHECKED_RETICS = 2;

	public static int FAST_SPEED_NUM_OF_CHECKED_RETICS = 1;
	/**
	 * indicates whether to remove min-weight triplet from each cycle
	 * permanently, or add it after the integer programming.
	 * 
	 * For now, we decided to have it false all the times, so all input triplets are presented in the network
	 **/
	public static final boolean REAMOVE_CYCLIC_TRIPLETS = false;

	/**
	 * indicate whether to remove a reticulation and perform the factorization
	 * to find any possible contractions, to : 1- refining left and right
	 * neighborhoods 2- refining reticulation candidates
	 * 
	 * as this operation is time consuming the default value is set to false
	 */
	public static boolean FAST_SPEED_CONTRACT_RETICS_CHECK = true;
	/**
	 * level 0 prints everything, higher levels prints less things
	 */
	public static int logLevel = 2;
	int maxW;

	public void doAlgorithm() {
		ths = this;
		String dotFilePath = tripletsFile + ".dot";
		if (gui != null) {
			gui.outputfileTxt.setText(dotFilePath);
			gui.outputfileTxt.setCaretPosition(gui.outputfileTxt.getText().length());

		}
		// leafVer = new HashMap<LeafSet, VertexModel>();
		// knownReticulations = new Vector<SubGraph>();
		tow = new Vector<Triplet>();
		Gf = new GraphModel();
		weights = new HashMap<Pair<Integer, Integer>, Integer>();
		// factorizationCache = new HashMap<Pair<LeafSet, Boolean>,
		// Vector<LeafSet>>();
		lastXi = 0;
		F0 = new HashMap<String, Integer>();
		// result = new GraphModel(true);
		algDepth = 0;
		// numOfExploredLeaves = 0;
		minInCons = Integer.MAX_VALUE;
		minInConsG = null;
		// GraphData gd = new GraphData(Application.blackboard);
		// gd.core.showGraph(network);
		EventUtils.algorithmStep(this, "Read input from file");
		println("reading triplets...");
		Vector<Triplet> rt = readTriplets();
		ths.tow = rt;
		println(tow.size() + " triplets are loaded.");
		if (ths.tow == null) {
			return;
		}
		// create tripletLookup table
		int n = Utils.getN(tow);
		numOfLeaves = n;
		// tripletLookup = Utils.createTripletLookupTable(tow);
		Gf = Utils.createIPGraph(tow); // old lingo code was: readIPInput();
		if (Gf == null) {
			println("Error: Input triplets have conflict!");
			ArrayList<Triplet> inco = getCycleTriplets(Utils.cycle);
			printCollection(inco);
			return;
		}

		LeafSet all = new LeafSet();
		for (SubGraph sg : getConnectedComponents(Gf)) {
			LeafSet ks = leafSet(sg);
			all.leaves.addAll(ks.leaves);
		}
		TripNetDS inp = new TripNetDS();
		inp.tow = tow;
		inp.leafset = all;
		inp.weights = weights;
		inp.maxW = maxW;
		// inp.weights =
		// gd.core.showGraph(Gf);
		// if (numOfComponents(Gf) > 1) {
		// Deque<LeafSet> comps = new ArrayDeque<LeafSet>();
		// all = new LeafSet();
		// for (SubGraph sg : getConnectedComponents(Gf)) {
		// LeafSet ks = leafSet(sg);
		// doAlgorithm(inp.getsubinstance(ks));
		// comps.add(ks);
		// all.leaves.addAll(ks.leaves);
		// }
		// // createTree(vertex(all), comps, new HashMap<LeafSet, SubGraph>());
		// //// todo: MOHEM
		// ##########################################################################
		// connect them #########################################
		// } else {
		// for (SubGraph sg : getConnectedComponents(Gf))
		inp.network = new GraphModel();
		vertex(inp.getsubinstance(inp.leafset));
		recurseAlgorithm(inp);
		// }
		if (gui != null) {
			dotFilePath = gui.outputfileTxt.getText();
		}

		println("Finishing ...");
		GraphModel network = inp.network;
		try {
			saveToDotFile(dotFilePath, network);

			// println("Checking consistency of the result network with the input triplets ...");
			// if (speed != SPEED_FAST) {
			println("Doing the last checks ...");

			Vector<Triplet> incons = ConsistensyChecker.consistencyCheck(inp.network, tow, false,
					null);
			boolean b = incons.size() == 0;
			println("Is Consistent:" + b);
			println(incons.size());
			for (Triplet t : incons)
				println(t);
			println(incons.size());
			if (speed != SPEED_FAST) {

				if (!b) {
					println("It seems that there is a problem in the algoritm resulting in incinsistent netwowks."
							+ "Please contact us sending your triplets to solve the problem.");
					while (incons.size() > 0) {
						insertEdgesToAddTriplets(inp.network, incons);
						incons = ConsistensyChecker.consistencyCheck(inp.network, tow, false, null);
					}
				}
				EventUtils.algorithmStep(this, "Is Consistent:" + b);
			}
		} catch (Exception e) {
			e.printStackTrace();
			println("NOT CONSISTENT");
		}
		// TextBox.showTextDialog("DOT output (use graphvis to see the network)",
		// dot);

	}

	public static void saveToDotFile(String dotFilePath, GraphModel network) {
		for (VertexModel v : network) {
			if (network.getOutDegree(v) != 0) {
				v.setLabel("");
				v.setSize(new GraphPoint(10, 10));
			} else {
				String l = v.getLabel();
				v.setLabel(l.length() <= 1 ? l : l.substring(1, l.length() - 1));
			}
		}
		// dumpResultGraph();
		String dot = printDot(network);
		// ptintln(dot);

		try {
			PrintWriter w = new PrintWriter(dotFilePath);
			w.print(dot);
			w.close();
			println(dotFilePath + " has been created.");
			println("Level:" + Utils.computeLevel(network));
			if (gui == null) {
				Utils.viewDotFile("\"" + tripletsFile.getAbsolutePath() + ".dot\"");
				// SaveSimpleGraph.save(inp.network, tripletsFile +
				// ".graph");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace(log);
			StaticUtils.addExceptionLog(e);
		} catch (IOException e) {
			e.printStackTrace(log);
			StaticUtils.addExceptionLog(e);
		} catch (InterruptedException e) {
			e.printStackTrace(log);
			StaticUtils.addExceptionLog(e);
		}
	}

	static ArrayList<Triplet> getCycleTriplets(LinkedList<VertexModel> cycle) {
		Iterator<VertexModel> it = cycle.iterator();
		VertexModel prev = it.next();
		ArrayList<Triplet> inco = new ArrayList<Triplet>();
		while (it.hasNext()) {
			VertexModel cur = it.next();
			EdgeModel e = Utils.dag.getEdge(prev, cur);
			inco.add((Triplet) e.getProp().obj);
			prev = cur;
		}
		inco.add((Triplet) Utils.dag.getEdge(cycle.getLast(), cycle.getFirst()).getProp().obj);
		return inco;
	}

	// int numOfExploredLeaves;
	GraphModel minInConsG;
	int minInCons;

	private static Reticulation findRightAndLeftChildAndCreateReticulation(
			Vector<LeafSet> lowestFactors, LeafSet reticulation, TripNetDS inp, int maxw) {

		int minw;// now we want to find right and left neighbours of
		// reticulation, for this, first we divide the LSs into
		// right and left neighbourhoods, for this we create a bipartite graph
		// based on maxw
		// if an LS's maxw is smaller than maxw, then it is a part of an inner
		// reticulation todo: is it true?
		HashSet<LeafSet> rightNeighbourhood = new HashSet<LeafSet>();
		HashSet<LeafSet> leftNeighbourhood = new HashSet<LeafSet>();
		HashSet<LeafSet>[] neighbourhood = new HashSet[] { rightNeighbourhood, leftNeighbourhood };
		// split the factors to a bigraph based on maxw, this will guide us to
		// find right and left neighbourhoods
		LeafSet cur = lowestFactors.get(0);
		if (cur == reticulation)
			cur = lowestFactors.get(1);
		cur.type = LeafSet.RIGHT;
		neighbourhood[LeafSet.RIGHT - 1].add(cur);
		Queue<LeafSet> q = new LinkedList<LeafSet>();
		q.add(cur);
		boolean warninged = false;
		while (!q.isEmpty()) { // a bfs queue
			cur = q.poll();
			for (LeafSet ls : lowestFactors) {
				if (ls == reticulation || ls == cur)
					continue;
				if (inp.getWeight(cur, ls).first == maxw) {
					int newType = cur.type == LeafSet.RIGHT ? LeafSet.LEFT : LeafSet.RIGHT;
					if (ls.type == LeafSet.NOT_SET) {
						ls.type = newType;
						q.add(ls);
						neighbourhood[newType - 1].add(ls);
					} else if (ls.type != newType) {
						// todo: LeafSets can not be 2-partitioned.
						if (!warninged) {
							println("Warning 2: LeafSets can not be 2-partitioned");
							warninged = true;
						} else {
							System.out.print(".");
						}

					}
				}
			}
		}

		// SubGraph reminder = Utils.getABackup(inp.createFGraph());//
		// _sg.getACopy();
		LeafSet reminder = inp.leafset.getACopy();
		reminder.remove(reticulation);
		TripNetDS remd = inp.getsubinstance(reminder);
		// removeFrom(reminder, reticulation);
		// todo: this didnt work, because it seperated "8 9 10, 5 12" as a
		// contracted seperable factor, and it ruins every thing
		// try to use contracted factors to refine right and left neighbourhoods
		if (speed < SPEED_FAST || FAST_SPEED_CONTRACT_RETICS_CHECK) {
			Pair<Vector<LeafSet>, Vector<LeafSet>> p2 = factorize(remd, false);
			Vector<LeafSet> remFactors = p2.first;
			Vector<LeafSet> contractedRemFactors = LeafSet.getContainingMembers(remFactors,
					lowestFactors);
			reticulation.numOfContractionsOnRemoval = contractedRemFactors.size();
			reticulation.contractionsOnRemoval = contractedRemFactors;
			if (contractedRemFactors.size() > 2) {
				println("Err 3: there is more than 2 contracted factors");
			} else if (contractedRemFactors.size() == 2) {
				// todo: in this case it was a galled network? right and left
				// can
				// precisely be found
				rightNeighbourhood = LeafSet.getContainedOrEqualMembers(
						contractedRemFactors.get(0), lowestFactors);
				leftNeighbourhood = LeafSet.getContainedOrEqualMembers(contractedRemFactors.get(1),
						lowestFactors);
			} else if (remFactors.size() == 1) {

			}
			boolean refineRightFactors = false;
			boolean refineLeftFactors = false;
			for (LeafSet ls : contractedRemFactors) {
				ls.mark = true;
				if (ls.type == LeafSet.RIGHT)
					refineRightFactors = true;
				if (ls.type == LeafSet.LEFT)
					refineLeftFactors = true;
			}
			// if at least one contained member was found remove all
			// non-contained
			// members from the related neighbourhood
			if (refineRightFactors) {
				Iterator<LeafSet> it = rightNeighbourhood.iterator();
				while (it.hasNext()) {
					LeafSet ls = it.next();
					if (ls.mark)
						it.remove();
				}
			}
			if (refineLeftFactors) {
				Iterator<LeafSet> it = leftNeighbourhood.iterator();
				while (it.hasNext()) {
					LeafSet ls = it.next();
					if (ls.mark)
						it.remove();
				}
			}
		}
		println("right Neighbourhood:");
		printCollection(rightNeighbourhood);
		println("left Neighbourhood:");
		printCollection(leftNeighbourhood);
		neighbourhood = new HashSet[] { rightNeighbourhood, leftNeighbourhood };
		// now the right and left childs are the lowest (w(reticulation, child))
		// members of right and left neighbourhood
		LeafSet[] child = new LeafSet[2];
		for (int i = 0; i <= 1; i++) {
			LeafSet c = null;
			minw = Integer.MAX_VALUE;
			for (LeafSet ls : neighbourhood[i]) {
				int w = inp.getWeight(ls, reticulation).second;
				if (w < minw) {
					minw = w;
					c = ls;
				}
			}
			child[i] = c;
		}
		LeafSet rightChild = child[LeafSet.RIGHT - 1];
		LeafSet leftChild = child[LeafSet.LEFT - 1];

		// now insert the reticulation to the recursively built network
		if (rightChild == null && leftChild == null)
			throw new RuntimeException("Left and Right child could NOT be found!");
		if (rightChild != null)
			println("Right Child: " + rightChild);
		else
			println("Right Child not found.");
		if (leftChild != null)
			println("Left Child: " + leftChild);
		else
			println("Left Child not found.");
		Reticulation r = new Reticulation(reticulation, rightChild, leftChild, inp.network);
		return r;
	}

	public static void recurseAlgorithm(TripNetDS inp) {
		boolean finished = false;
		while (!finished) {
			finished = true;
			for (VertexModel v : inp.network) {
				if (inp.network.getOutDegree(v) == 0) {
					TripNetDS inpr = getTripNetInstance(v);
					if (inpr != null) {
						finished = false;
						inpr.network = new GraphModel(true);
						VertexModel root = doAlgorithm(inpr);
						setTripNetInstance(v, null);
						SubGraph subg = Utils.getABackup(inpr.network);
						GraphModel parent = inp.network;
						// Utils.view(inp.network);
						// now place subg into parent, consider root as v
						for (VertexModel _ : subg.vertices) {
							if (_ != root)
								parent.addVertex(_);
						}
						for (EdgeModel _ : subg.edges) {
							EdgeModel t = _;
							if (t.source == root) {
								t = new EdgeModel(t, v, t.target);
							}
							if (t.target == root)
								t = new EdgeModel(t, t.source, v);
							parent.insertEdge(t);
						}
						break;
					} else if (getLeafSet(v).leaves.size() > 1) {
						println("incmplt");
					}
				}
			}
		}
	}

	public static final int SPEED_FAST = 3, SPEED_NORMAL = 2, SPEED_SLOW = 1;
	public static int speed = SPEED_SLOW;

	static int dbgOfIncsChk = 0;

	public static VertexModel doAlgorithm(final TripNetDS inp) {
		algDepth++;
		if (inp.network == null)
			inp.network = new GraphModel(true);
		if (inp.leafset.leaves.size() == 1) {
			algDepth--;
			return vertex(inp.leafset, inp.network);
		}
		// Utils.completeEdges(_sg);
		println("*************************");
		println("Starting Algorithm On:" + inp.leafset);
		// GraphData gd = new GraphData(Application.blackboard);
		// gd.core.showGraph(g);
		// if (numOfComponents(g) != 1)
		// throw new RuntimeException("Graph is not connected!");

		VertexModel root = buildAhoTree_OneLevel(inp);
		if (root != null) {
			algDepth--;
			return root;
		}

		// contract everything
		Pair<Vector<LeafSet>, Vector<LeafSet>> _p = factorize(inp, true);
		Vector<LeafSet> _allFactors = _p.first;
		final TripNetDS contractedInp = inp.getContractedInstance(_allFactors);
		println("Contraction:");
		for (int i : contractedInp.leafset.leaves) {
			println(i + "->" + inp.leafset.contractedFrom.get(i));
		}
		RunOnReticulationSelectionFound listener = new RunOnReticulationSelectionFound() {
			void reticulationSelectionFound(final Pair<TripNetDS, Vector<Reticulation>> rs) {
				try {
					doAlgorithm(rs.first);
					recurseAlgorithm(rs.first);
					// buildAhoTree(rs.first);
				} catch (Exception e) {
					// e.printStackTrace();
					println("Err 100: Not Tree!");
					return;
				}

				// add reticulation leaf to reminder

				try {
					addReticulationsToNetwork(rs.first, rs.second, contractedInp);
				} catch (Exception e) {
					StaticUtils.addExceptionLog(e);
					return;
				}
				recurseAlgorithm(rs.first);

				final int[] reticsNum = new int[] { countRetics(rs.first.network) };
				if (minFoundRetics <= reticsNum[0])
					return;
				// add edges to correct inconsistencies

				try {
					PrintWriter w = new PrintWriter("a.dot");
					w.write(TripNet.printDot(rs.first.network));
					w.close();

					println("checking consistency of subnetwork.");
					if (speed != SPEED_FAST || true) {
						ConsistensyChecker.consistencyCheck(rs.first.network, contractedInp.tow,
								false, 	new ConsistencyCheckListener() {
									public boolean inconsistentTripletFound(Triplet t) {
										dbgOfIncsChk++;
//										saveToDotFile(dbgOfIncsChk + " " + t + " before.dot",
//												rs.first.network);
										println("Inconsistant triplet: " + t);
										reticsNum[0]++;
										if (minFoundRetics <= reticsNum[0])
											return true;

										insertEdgeToAddTriplet(t, rs.first.network);
										isNetworkChanged = true;
										//saveToDotFile(dbgOfIncsChk + " " + t + " cafter.dot",
										//		rs.first.network);
										return false;
									}

								});
					}
					if (minFoundRetics <= reticsNum[0])
						return;
				} catch (Exception e) {
					StaticUtils.addExceptionLog(e);
					println("Err 101: Network is not complete!");
					return;
				}

				GraphModel network = rs.first.network;
				int lvl = countRetics(network);
				if (lvl < minFoundRetics) {
					minFoundRetics = lvl;
					min = rs.first;
				}

			}

			void finished() {

			}
		};
		findReticulationsHuristically(contractedInp, listener);
		inp.expandContractedInstance(listener.min);
		algDepth--;
		return getRoot(inp);
	}

	static EdgeModel getHighestCutArc(VertexModel start, GraphModel network) {
		if (network.getInDegree(start) == 0)
			return null;
		GraphModel U = Utils.createUndirectedCopy(network);
		Deque<VertexModel> parents = getParentsToRoot(start, network);
		VertexModel p;
		Iterator<VertexModel> parent = parents.iterator();
		Iterator<VertexModel> child = parents.iterator();
		child.next();

		while (child.hasNext()) {
			p = parent.next();
			VertexModel c = child.next();
			EdgeModel e = network.getEdge(p, c);
			getLeafSet(c);
			if (isDisconnected(U, Utils.getDup(e)))
				return e;
		}
		return null;
	}

	static Vector<EdgeModel> getAllHighestCutArcs_usingLeafSets(VertexModel start,
			GraphModel network, LeafSet all, Vector<Triplet> tow) {
		Vector<EdgeModel> ret = new Vector<EdgeModel>();
		if (network.getInDegree(start) == 0)
			return null;
		Deque<VertexModel> parents = getParentsToRoot(start, network);
		VertexModel p;
		Iterator<VertexModel> parent = parents.descendingIterator();
		Iterator<VertexModel> child = parents.descendingIterator();
		parent.next();

		while (parent.hasNext()) {
			p = parent.next();
			VertexModel c = child.next();
			EdgeModel e = network.getEdge(p, c);
			if (isNonSeperable(all, getLeafSet(p), tow))
				ret.add(e);
		}

		return ret.size() > 0 ? ret : null;
	}

	/**
	 * root comes first
	 */
	private static Deque<VertexModel> getParentsToRoot(VertexModel start, GraphModel network) {
		Deque<VertexModel> parents = new LinkedList<VertexModel>();
		VertexModel p = getParent(start, network);

		while (network.getInDegree(p) > 0) {
			parents.addFirst(p);
			p = network.getBackNeighbours(p).iterator().next();
		}
		parents.addFirst(p);
		return parents;
	}

	static boolean isDisconnected(GraphModel g, EdgeModel discardedEdge) {
		g.removeEdge(discardedEdge);
		ArrayList<SubGraph> comps = getConnectedComponents(g);
		g.addEdge(discardedEdge);
		if (comps.size() > 1)
			return true;
		return false;
	}

	private static int countRetics(GraphModel network) {
		int lvl = 0;// Utils.computeLevel(rs.first.network);
		for (VertexModel v : network) {
			if (network.getOutDegree(v) == 1 && network.getInDegree(v) == 2)
				lvl++;
		}
		return lvl;
	}

	static VertexModel getRoot(TripNetDS inp) {
		for (VertexModel v : inp.network) {
			if (inp.network.getInDegree(v) == 0)
				return v; // as the network root
		}
		return null;
	}


	private static void findReticulationsHuristically(TripNetDS contractedInp,
			RunOnReticulationSelectionFound listener) {
		// reticulations must be found and removed from reminder until it turns
		// to a AHO tree
		Vector<Reticulation> reticulations = new Vector<Reticulation>();
		TripNetDS reminder = contractedInp.getsubinstance(contractedInp.leafset);
		Pair<Vector<LeafSet>, Vector<LeafSet>> p = factorize(reminder, false);
		Vector<LeafSet> lowestFactors = p.second;
		Queue<ReticulationInstance> toFind = new LinkedList<ReticulationInstance>();
		ReticulationInstance ri = new ReticulationInstance(reminder, reticulations, lowestFactors);
		toFind.add(ri);
		// each member of to find is an instance of reticulation finding problem
		while (!toFind.isEmpty()) {
			ReticulationInstance pair = toFind.poll();
			reminder = pair.reminder;
			reticulations = pair.reticulations;
			if (reticulations.size() >= listener.minFoundRetics)
				continue; // this case will not lead to a lower level network
			lowestFactors = pair.lowestFactors;

			boolean isTree = isAhoDecomposable(reminder);
			while (!isTree) {
				// if (allFactors == null) allFactors = p.first;
				// p = factorize(reminder);
				// lowestFactors = p.second;

				Vector<Reticulation> suitableRetics = findSuitableReticulations(lowestFactors,
						contractedInp);
				if (suitableRetics.size() > 1) {
					for (Reticulation r : suitableRetics) {
						if (r != suitableRetics.get(0)) {
							TripNetDS _reminder = reminder.getACopyWithEmptyNetwork();
							Vector<Reticulation> _reticulations = new Vector<Reticulation>(
									reticulations);
							_reticulations.add(0, r);

							LeafSet remls = _reminder.leafset;
							remls.leaves.removeAll(r.reticulate.leaves);
							_reminder = _reminder.getsubinstance(remls);
							isTree = isAhoDecomposable(_reminder);
							if (isTree) {
								Pair<TripNetDS, Vector<Reticulation>> pair1 = new Pair<TripNetDS, Vector<Reticulation>>(
										_reminder, _reticulations);
								// reticulationSelections.add(pair1);
								listener.reticulationSelectionFound(pair1);
							} else {
								// Vector<LeafSet> _lowestFactors = new
								// Vector<LeafSet>(lowestFactors);
								// _lowestFactors.remove(r.reticulate);
								// //add one without contracted components
								// toFind.add(new
								// ReticulationInstance(_reminder.getACopyWithEmptyNetwork(),
								// new Vector<Reticulation>(_reticulations),
								// _lowestFactors));
								println("finding reticulations on" + _reminder.leafset);
								p = factorize(_reminder, false);
								Vector<LeafSet> _lowestFactors = p.second;
								// and one with contracted components
								ri = new ReticulationInstance(_reminder, _reticulations,
										_lowestFactors);
								// if (!seen.contains(ri)) {
								// seen.add(ri);
								if (ri.reticulations.size() < listener.minFoundRetics)
									toFind.add(ri);
								// }
							}
						}
					}
				}

				Reticulation r = suitableRetics.get(0);
				println("considering as reticulation:" + r.reticulate);
				// continue with the contracted one
				reticulations.add(0, r); // todo: can done faster using better
				// DS or algorithm
				// allFactors.remove(r.reticulate);
				LeafSet remls = reminder.leafset;
				remls.leaves.removeAll(r.reticulate.leaves);
				reminder = reminder.getsubinstance(remls);

				Vector<LeafSet> _lowestFactors = new Vector<LeafSet>(lowestFactors);
				_lowestFactors.remove(r.reticulate);
				// and add one without contracted components
				// ri = new
				// ReticulationInstance(reminder.getACopyWithEmptyNetwork(), new
				// Vector<Reticulation>(reticulations), _lowestFactors);
				// if (!seen.contains(ri)) {f
				// seen.add(ri);
				// toFind.add(ri);
				// }

				p = factorize(reminder, false);
				lowestFactors = p.second;

				isTree = isAhoDecomposable(reminder);
			}
			Pair<TripNetDS, Vector<Reticulation>> pair1 = new Pair<TripNetDS, Vector<Reticulation>>(
					reminder, reticulations);
			// reticulationSelections.add(pair1);
			listener.reticulationSelectionFound(pair1);
		}
		listener.finished();
		// return reticulationSelections;
	}

	/**
	 * Finds suitable reticulations for the given input.
	 */
	@SuppressWarnings("unchecked")
	static Vector<Reticulation> findSuitableReticulations(Vector<LeafSet> lowestFactors,
			TripNetDS inp) {
		println("Finding suitable reticulations on" + lowestFactors);

		int maxRetSize = Integer.MAX_VALUE;
		if (speed == SPEED_NORMAL)
			maxRetSize = NORMAL_SPEED_NUM_OF_CHECKED_RETICS;
		if (speed == SPEED_FAST)
			maxRetSize = FAST_SPEED_NUM_OF_CHECKED_RETICS;

		int maxw;
		int minw;
		for (LeafSet ls : lowestFactors) {
			ls.maxw = Integer.MIN_VALUE;
			ls.minw = Integer.MAX_VALUE;
		}

		Pair<Integer, Integer> pm = findMaxMinWeights(inp, lowestFactors);
		maxw = pm.first;
		minw = pm.second;
		int minimumOfmaxw = Integer.MAX_VALUE;
		println("********************");
		for (LeafSet ls : lowestFactors) {
			if (ls.minw == minw)
				minimumOfmaxw = Math.min(minimumOfmaxw, ls.maxw);
			if (logLevel == 0)
				println(ls.maxw + " , " + ls.minw + "\t: " + ls);
		}
		// the reticulation is a LS which it's minw and maxw are minimum
		// todo: what to do in a cherry reticulation case?
		LeafSet reticulation = null;
		Vector<LeafSet> suitableReticulations = new Vector<LeafSet>();
		for (LeafSet ls : lowestFactors) {
			if (ls.minw == minw && ls.maxw == minimumOfmaxw) {
				suitableReticulations.add(ls);
			}
		}
		reticulation = suitableReticulations.get(0);
		if (suitableReticulations.size() > 1)
			println("Warning 1: Reticulation can not uniquely be found.");

		// //todo: use is_tree here #DIDN't work :( for 3 vertices networks
		// //in galled networks removing the reticulation causes the network to
		// become a tree
		// //using this fact, the reticulation can uniquely be found
		// LeafSet galledRetic = null;
		// if (suitableReticulations.size() > 1) {
		// for (LeafSet ls : suitableReticulations) {
		// LeafSet reminder = inp.leafset.getACopy();
		// reminder.remove(ls);
		// if (isAhoDecomposable(inp.getsubinstance(reminder))){
		// galledRetic=ls;
		// break;
		// }
		// }
		// }
		// if (galledRetic!=null)
		// reticulation=galledRetic;

		println("suitable reticulation(s): " + suitableReticulations);

		if (speed > SPEED_SLOW
				&& suitableReticulations.size() > maxRetSize) {

			// for these cases we want to reduce our search space for finding
			// reticulations
			// we use some heuristics to select better candidates of
			// reticulations

			/**
			 * first heuristic: as you mentioned earlier we have a condition on
			 * leafsets to put them in suitable reticulations: ls.minw == minw
			 * && ls.maxw == minimumOfmaxw
			 * 
			 * here we count that for a suitable reticulation how many times
			 * this minw occured to other suitable reticulations (calling
			 * minwOccurance), a candidate with higher number of occurances of
			 * minw is a better candidate.
			 * 
			 * another heuristic but a weaker one is maxwOccarance. we seek
			 * candidates with lower maxwOcc...
			 * 
			 * as a general rule, a reticulation must have lower weights to
			 * other components, because it's common parent with all other
			 * components is somewhere lower than the root.
			 */

			int maxOfminwOcurrance = Integer.MIN_VALUE;
			for (LeafSet ls1 : suitableReticulations) {
				ls1.minwOcurrance = 0;
				for (LeafSet ls2 : suitableReticulations) {
					if (inp.getWeight(ls1, ls2).second == minw) {
						ls1.minwOcurrance++;
					}
				}
				maxOfminwOcurrance = Math.max(maxOfminwOcurrance, ls1.minwOcurrance);
			}

			Vector<LeafSet> reticulationCandidates = new Vector<LeafSet>();

			for (LeafSet ls1 : suitableReticulations) {
				if (ls1.minwOcurrance == maxOfminwOcurrance) {
					reticulationCandidates.add(ls1);
				}
				println(ls1 + ": mincount: " + ls1.minwOcurrance + ", maxcount: "
						+ ls1.maxwOcurrance);
			}

			if ((speed == SPEED_FAST && reticulationCandidates.size() <= FAST_SPEED_NUM_OF_CHECKED_RETICS)
					|| speed == SPEED_NORMAL) {
				suitableReticulations = reticulationCandidates;
			} else {
				// here we take a look at maxwOcurrance
				int minOfMaxwOcurrance = Integer.MAX_VALUE;
				for (LeafSet ls : reticulationCandidates) {
					minOfMaxwOcurrance = Math.min(minOfMaxwOcurrance, ls.maxwOcurrance);
				}
				Vector<LeafSet> reticulationCandidates2 = new Vector<LeafSet>();
				for (LeafSet ls : reticulationCandidates) {
					if (minOfMaxwOcurrance == ls.maxwOcurrance) {
						reticulationCandidates2.add(ls);
					}
				}

				if ((speed == SPEED_FAST && reticulationCandidates2.size() <= FAST_SPEED_NUM_OF_CHECKED_RETICS)
						|| speed == SPEED_NORMAL
						&& reticulationCandidates2.size() <= NORMAL_SPEED_NUM_OF_CHECKED_RETICS) {
					suitableReticulations = reticulationCandidates2;
				} else {
					/**
					 * here we need further huristics for selection of better
					 * reticulation candidates
					 */
					suitableReticulations = reticulationCandidates2;

					// here we want to perform some heuristic functions to
					// choose better
					// candidates of reticulations
					// these heuristics are not meant to be exact, but it feels
					// that
					// they work fine in most cases

					// 1- Normally if we remove a reticulation some contractions
					// must
					// occur in the reminder,
					// so if this happened remove uncontracting candidates

					// ############################ here :))

					// for (LeafSet ls : suitableReticulations) {
					// LeafSet reminder = inp.leafset.getACopy();
					// reminder.remove(ls);
					// Pair<Vector<LeafSet>, Vector<LeafSet>> p2 =
					// factorize(reminder,
					// false);
					// Vector<LeafSet> remFactors = p2.first;
					// Vector<LeafSet> contractedRemFactors = LeafSet
					// .getContainingMembers(remFactors, lowestFactors);
					//
					// }

				}

			}

		}

		Vector<Reticulation> ret = new Vector<Reticulation>();
		LeafSet allRetics = new LeafSet();
		int maxContractedCompsonRemoval = Integer.MIN_VALUE;
		for (LeafSet ls : suitableReticulations) {
			allRetics.leaves.addAll(ls.leaves);
			println("Analyzing reticulation: " + ls);
			// this updates ls.numOfContractionsOnRemoval
			Reticulation r = findRightAndLeftChildAndCreateReticulation(lowestFactors, ls, inp,
					maxw);
			maxContractedCompsonRemoval = Math.max(maxContractedCompsonRemoval,
					ls.numOfContractionsOnRemoval);
			ls.r = r;

			ret.add(r);
		}

		if (speed < SPEED_NORMAL || maxContractedCompsonRemoval == 0
				|| maxContractedCompsonRemoval > 2 || suitableReticulations.size() == 1) {
			if (ret.size() > maxRetSize) {
				Vector<Reticulation> ret2 = new Vector<Reticulation>();
				for (int i = 0; i < maxRetSize; i++) {
					ret2.add(ret.get(i));
				}
				return ret2;
			} else
				return ret;
		}

		// and in the other cases we have:
		/**
		 * now we continue the heuristic candidates story
		 */
		Vector<LeafSet> candidates = new Vector<LeafSet>();
		/**
		 * this heuristic says that only put candidates which their removal lead
		 * to contraction in remainder components
		 */
		println("### contraction helps to better determination of reticulation candidates.");
		for (LeafSet ls : suitableReticulations) {
			println("num of contractions on removal of " + ls + ": "
					+ ls.numOfContractionsOnRemoval);
			if (ls.numOfContractionsOnRemoval == maxContractedCompsonRemoval) {
				candidates.add(ls);
			}
		}
		if (candidates.size() <= maxRetSize) {
			for (LeafSet ls : candidates)
				ret.add(ls.r);
			return ret;
		}

		// and otherwise:
		/**
		 * in this heuristic we try to count number of inconsistent triplets in
		 * each selection of reticulation. this is really a heuristic function
		 * and may surprise you. but we think that it provides a good etimation
		 * of how much a candidate is good or bad.
		 */
		Vector<LeafSet> candidates2 = new Vector<LeafSet>();
		for (LeafSet r : candidates) {
			LeafSet A, B;
			A = r.contractionsOnRemoval.get(0);
			int inco = 0;
			if (maxContractedCompsonRemoval == 2) {
				B = r.contractionsOnRemoval.get(1);
				for (Triplet t : inp.tow) {
					if ((A.leaves.contains(t.i) && B.leaves.contains(t.j) && r.leaves.contains(t.k))
							|| (A.leaves.contains(t.j) && B.leaves.contains(t.i) && r.leaves
									.contains(t.k))) {
						inco++;
					}
				}

			} else {
				for (Triplet t : inp.tow) {
					if ((A.leaves.contains(t.i) && !A.leaves.contains(t.j)
							&& !r.leaves.contains(t.j) && r.leaves.contains(t.k))
							|| (A.leaves.contains(t.j) && !A.leaves.contains(t.i)
									&& !r.leaves.contains(t.i) && r.leaves.contains(t.k))) {
						inco++;
					}
				}

			}
			r.numOfInconcsH = inco;
			println("Approx. num of inconsistant triplets on removal of " + r.leaves + ": " + inco);
		}

		ret.clear();
		for (LeafSet ls : candidates) {
			ret.add(ls.r);
			if (ret.size() == maxRetSize) {
				return ret;
			}
		}
		return ret;

		// todo: relook
		// check also the cherry case
		// if (suitableReticulations.size() > 1 && speed <= SPEED_NORMAL) {
		// println("cherry " + allRetics);
		// Vector<LeafSet> _lf = new Vector<LeafSet>(lowestFactors);
		// for (LeafSet ls : suitableReticulations) {
		// _lf.remove(ls);
		// }
		// if (_lf.size() > 0) {
		// Reticulation cherry = findRightAndLeftChildAndCreateReticulation(_lf,
		// allRetics,
		// inp, maxw);
		// ret.add(cherry);
		// }
		// }
		//
		// return ret;
	}

	/**
	 * fills in ret all subsets of 1 to n
	 */
	static void allSubSets(int n, Vector<Integer[]> ret, Vector<Integer> current) {
		if (n == 0) {
			Integer[] ss = new Integer[n];
			ret.add(current.toArray(ss));
		} else {
			allSubSets(n - 1, ret, current);
			current.add(n);
			allSubSets(n - 1, ret, current);
			current.removeElement(n);
		}
	}

	static void allPermutations(int n, Vector<Integer[]> ret, Integer[] current) {
		if (n == 0) {
			ret.add(current.clone());
			return;
		} else {
			allPermutations(n - 1, ret, current);
			for (int i = 0; i < n - 1; i++) {
				int t = current[i];
				current[i] = current[n - 1];
				current[n - 1] = t;
				allPermutations(n - 1, ret, current);
				t = current[i];
				current[i] = current[n - 1];
				current[n - 1] = t;
			}
		}
	}

	// private static Vector<Pair<TripNetDS, Vector<Reticulation>>>
	// findReticulationsAllCases(TripNetDS contractedInp, TripNetDS inp) {
	// //reticulations must be found and removed from reminder until it turns to
	// a AHO tree
	// HashSet<Integer[]> perms=new HashSet<Integer[]>();
	//
	// Pair<Vector<LeafSet>, Vector<LeafSet>> p = factorize(reminder);
	// Vector<LeafSet> lowestFactors = p.second;
	//
	// int n = lowestFactors.size();
	// Vector<Integer[]> als = new Vector<Integer[]>();
	// allSubSets(n, als, new Vector<Integer>() );
	// for (Integer[] subset:als){
	// Vector<Integer[]> alp = new Vector<Integer[]>();
	// allPermutations(subset.length, alp, subset);
	// for (Integer[] perm: alp){
	// Vector<Reticulation> reticulations = new Vector<Reticulation>();
	// TripNetDS reminder = contractedInp.getsubinstance(contractedInp.leafset);
	// for (int i:perm){
	// boolean isTree = isAhoDecomposable(reminder);
	// if (isTree){
	// break; //todo: what to do after break?
	// }
	// LeafSet reticulation=lowestFactors.get(i);
	// Reticulation r =
	// findRightAndLeftChildAndCreateReticulation(lowestFactors, reticulation,
	// inp, maxw);
	//
	// int maxw;
	// int minw;
	// for (LeafSet ls : lowestFactors) {
	// ls.maxw = Integer.MIN_VALUE;
	// ls.minw = Integer.MAX_VALUE;
	// }
	//
	//
	// Pair<Integer, Integer> pm = findMaxMinWeights(inp, lowestFactors);
	// maxw = pm.first;
	// minw = pm.second;
	// println("********************");
	// for (LeafSet ls : lowestFactors) {
	// println(ls.maxw + " , " + ls.minw + "\t: " + ls);
	// }
	//
	// Reticulation r =
	// findRightAndLeftChildAndCreateReticulation(lowestFactors, reticulation,
	// inp, maxw);
	//
	// }
	// println();
	// }
	// }
	// Vector<LeafSet> allFactors = null;
	// boolean isTree = false;
	// while (!isTree) {
	// // Pair<Vector<LeafSet>, Vector<LeafSet>> p = factorize(reminder);
	// // Vector<LeafSet> lowestFactors = p.second;
	// // if (allFactors == null) allFactors = p.first;
	//
	// Reticulation r = findSuitableReticulations(lowestFactors, contractedInp);
	// reticulations.add(0, r);
	// // allFactors.remove(r.reticulate);
	// LeafSet remls = reminder.leafset;
	// remls.leaves.removeAll(r.reticulate.leaves);
	// reminder = reminder.getsubinstance(remls);
	// isTree = isAhoDecomposable(reminder);
	// }
	// Vector<Pair<TripNetDS, Vector<Reticulation>>> reticulationSelections =
	// new Vector<Pair<TripNetDS, Vector<Reticulation>>>();
	// reticulationSelections.add(new Pair<TripNetDS,
	// Vector<Reticulation>>(reminder, reticulations));
	// return reticulationSelections;
	// }

	// private static void findReticulationsAllChoices_rec(TripNetDS reminder,
	// Vector<Reticulation> selectedReticulations, Vector<Pair<TripNetDS,
	// Vector<Reticulation>>> reticulationSelections) {
	// if (isAhoDecomposable(reminder)) {
	// reticulationSelections.add(new Pair<TripNetDS,
	// Vector<Reticulation>>(reminder, selectedReticulations));
	// return;
	// }
	// int leaf = reminder.leafset.leaves.iterator().next();
	// selectedReticulations.add(new Reticulation())
	// }

	// private static Vector<Pair<TripNetDS, Vector<Reticulation>>>
	// findReticulationsAllChoices(TripNetDS contractedInp) {
	// //reticulations must be found and removed from reminder until it turns to
	// a AHO tree
	// Vector<Reticulation> reticulations = new Vector<Reticulation>();
	// TripNetDS reminder = contractedInp.getsubinstance(contractedInp.leafset);
	// boolean isTree = false;
	// while (!isTree) {
	// Pair<Vector<LeafSet>, Vector<LeafSet>> p = factorize(reminder);
	// Vector<LeafSet> lowestFactors = p.second;
	// // if (allFactors == null) allFactors = p.first;
	//
	// Reticulation r = findSuitableReticulations(lowestFactors, contractedInp);
	// reticulations.add(0, r);
	// // allFactors.remove(r.reticulate);
	// LeafSet remls = reminder.leafset;
	// remls.leaves.removeAll(r.reticulate.leaves);
	// reminder = reminder.getsubinstance(remls);
	// isTree = isAhoDecomposable(reminder);
	// }
	// Vector<Pair<TripNetDS, Vector<Reticulation>>> reticulationSelections =
	// new Vector<Pair<TripNetDS, Vector<Reticulation>>>();
	// reticulationSelections.add(new Pair<TripNetDS,
	// Vector<Reticulation>>(reminder, reticulations));
	// return reticulationSelections;
	// }

	static void removeFrom(SubGraph parent, SubGraph subGraph) {
		for (VertexModel v : subGraph.vertices) {
			if (OLDCODES.contains(parent, Xi(v))) {
				VertexModel vv = getVertex(parent, Xi(v));
				parent.vertices.remove(vv);
				Vector<EdgeModel> toremove = new Vector<EdgeModel>();
				for (EdgeModel e : parent.edges) {
					if (e.source == vv || e.target == vv)
						toremove.add(e);
				}
				for (EdgeModel e : toremove) {
					parent.edges.remove(e);
				}
			}
		}
	}

	// --------------------------------- I O
	// --------------------------------------
	public static Vector<Triplet> readTriplets() {
		Scanner s = null;
		Scanner w = null;
		try {
			s = new Scanner(tripletsFile);
			File weights = new File(tripletsFile.getAbsolutePath() + " - weights");
			if (weights.exists()) {
				w = new Scanner(weights);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return readTriplets(s, w);
	}

	/**
	 * 
	 * @param s
	 *            the triplets, one in each line
	 * @param weights
	 *            refers to the weights of the triplets, in each line the weight
	 *            of the triplet in the same line of the triplets file
	 * @return
	 */
	static Vector<Triplet> readTriplets(Scanner s, Scanner weights) {
		Vector<Triplet> tow = new Vector<Triplet>();
		try {
			while (s.hasNext()) {
				Triplet t = new Triplet(s.nextInt(), s.nextInt(), s.nextInt());
				if (weights != null) {
					t.w = weights.nextDouble();
//					if (t.w > TRIPLET_WEIGHT_MIN_BOUND)
						tow.add(t);
				} else
					tow.add(t);
			}
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
			println("Wrong input format: " + s.nextLine());
			return null;
		}
		return tow;
	}

	public static GraphModel readIPInput() {
		GraphModel ret = new GraphModel(false);
		ret.setDrawEdgeLabels(true);
		ret.setDrawVertexLabels(true);
		try {
			Scanner s = new Scanner(integerProgrammingFile);
			int n = 0;
			while (s.hasNext()) { // determine number of vertices
				String X = s.next();
				int h = (int) s.nextDouble();
				s.nextLine();
				int x1 = Integer.parseInt(X.substring(1, 3));
				int x2 = Integer.parseInt(X.substring(3));
				n = Math.max(x2, Math.max(n, x1));
			}
			VertexModel V[] = new VertexModel[n + 1]; // construct the graph
			for (int i = 0; i <= n; i++) {
				V[i] = new VertexModel();
				V[i].setLabel(i + "");
				setVertexId(V[i], i);
			}
			lastXi = n;
			ret.insertVertices(V);
			ret.removeVertex(V[0]);
			s = new Scanner(integerProgrammingFile);
			while (s.hasNext()) {
				String X = s.next();
				int h = (int) s.nextDouble();
				s.nextLine();
				int x1 = Integer.parseInt(X.substring(1, 3));
				int x2 = Integer.parseInt(X.substring(3));
				EdgeModel e = new EdgeModel(V[x1], V[x2]);
				e.setWeight(h);
				ths.F0.put(x1 + "," + x2, h);
				e.setShowWeight(true);
				ret.insertEdge(e);
			}
			s.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// put vertices around a circle
		// circular.circularVisualize(500, 500, ret);
		return ret;
	}

	public static Integer getW(int xi, int xj) {
		Integer w = ths.F0.get(xi + "," + xj);
		if (w == null)
			w = ths.F0.get(xj + "," + xi);
		if (w == null) {
			println("NULL WEIGHT!");
		}
		return w;
	}

	static VertexModel getVertex(SubGraph sg, Integer xi) {
		for (VertexModel v : sg.vertices) {
			if (xi == Xi(v))
				return v;
			// if (getRelatedSubGraph(v) != null) {
			// return getVertex(getRelatedSubGraph(v), xi);
			// }
		}
		return null;
	}

	/**
	 * @return Number of connected components of the given graph
	 */
	public static int numOfComponents(GraphModel graph) {
		double[][] mat = graph.getAdjacencyMatrix().getArray();
		int size = mat.length;
		ArrayList untraversed = new ArrayList();
		for (int i = 0; i < size; i++)
			untraversed.add(new Integer(i));

		ArrayList comps = new ArrayList();
		int parent[] = new int[size];
		for (int i = 0; i < size; i++)
			parent[i] = -1;

		ArrayList visit;
		for (; untraversed.size() > 0; untraversed.removeAll(visit)) {
			visit = new ArrayList();
			int currentNode = ((Integer) untraversed.get(0)).intValue();
			parent[currentNode] = currentNode;
			AlgorithmUtils.dfs((BaseGraph) graph, currentNode, visit, parent);
			comps.add(visit);
		}
		return comps.size();
	}

	public static void removeEdgesWithWeight(final int w, final GraphModel g) {
		// AbstractGraphRenderer.getCurrentGraphRenderer(Application.blackboard).ignoreRepaints(new
		// Runnable() {
		// public void run() {
		Vector<EdgeModel> toRemove = new Vector<EdgeModel>();
		for (EdgeModel e : g.getEdges()) {
			if (e.getWeight() == w)
				toRemove.add(e);
		}
		for (EdgeModel e : toRemove) {
			// println("removed: "+e);
			g.removeEdge(e);
		}
		// }
		// });
	}

	public static ArrayList<SubGraph> getNonSepComps(GraphModel g, Vector<Triplet> tow) {
		ArrayList<SubGraph> ret = new ArrayList<SubGraph>();
		for (SubGraph sg : getConnectedComponents(g))
			if (isNonSeperable(g, leafSet(sg), tow))
				ret.add(sg);
		return ret;
	}

	public static void removeMaxWeightedEdges(SubGraph sg) {
		int max = -1;
		ArrayList<EdgeModel> toRemove = new ArrayList<EdgeModel>();
		for (EdgeModel e : sg.edges)
			if (max <= e.getWeight()) {
				toRemove.add(e);
				max = e.getWeight();
			}
		for (EdgeModel e : toRemove)
			if (max == e.getWeight()) {
				sg.graph.removeEdge(e);
				sg.edges.remove(e);
			}
	}

	public static int getMaxWeight(GraphModel g) {
		int ret = -1;
		for (EdgeModel e : g.getEdges()) {
			// if (!e.getMark())
			ret = Math.max(ret, e.getWeight());
		}
		return ret;
	}

	public static ArrayList<SubGraph> getConnectedComponents(GraphModel g) {
		ArrayList<SubGraph> ret = new ArrayList<SubGraph>();
		VertexModel[] V = g.getVertexArray();
		ArrayList<ArrayList<Integer>> cc = getConnectedComponents2(g);
		for (ArrayList<Integer> al : cc) {
			SubGraph sg = new SubGraph(g);
			for (int i : al) {
				sg.vertices.add(V[i]);
				for (int j : al)
					if (g.isEdge(V[i], V[j]))
						sg.edges.add(g.getEdge(V[i], V[j]));
			}
			ret.add(sg);
		}
		return ret;
	}

	/**
	 * @return connected components of the given graph, each cell of ArrayList
	 *         is a ArrayList containing indices of the corresponding component
	 *         vertices indices
	 */
	public static ArrayList<ArrayList<Integer>> getConnectedComponents2(GraphModel graph) {
		double[][] mat = graph.getAdjacencyMatrix().getArray();
		int size = mat.length;
		ArrayList<Integer> untraversed = new ArrayList<Integer>();
		for (int i = 0; i < size; i++)
			untraversed.add(new Integer(i));

		ArrayList<ArrayList<Integer>> comps = new ArrayList();
		int parent[] = new int[size];
		for (int i = 0; i < size; i++)
			parent[i] = -1;

		ArrayList<Integer> visit;
		for (; untraversed.size() > 0; untraversed.removeAll(visit)) {
			visit = new ArrayList();
			int currentNode = ((Integer) untraversed.get(0)).intValue();
			parent[currentNode] = currentNode;
			AlgorithmUtils.dfs((BaseGraph) graph, currentNode, visit, parent);
			comps.add(visit);
		}
		return comps;
	}

	public static boolean isNonSeperable(GraphModel parentGraph, LeafSet sg, Vector<Triplet> tow) {

		int maxsgleaves = -1;
		for (int i : sg.leaves)
			maxsgleaves = Math.max(maxsgleaves, i);
		// this is a lookup table for the leaves in sg
		boolean sgleaves[] = new boolean[++maxsgleaves];
		for (int i : sg.leaves) {
			sgleaves[i] = true;
		}

		int maxpleaves = -1;
		for (VertexModel v : parentGraph) {
			if (Xi(v) != null)
				maxpleaves = Math.max(maxpleaves, Xi(v));
		}
		// this is a lookup table for the leaves in parent graph
		boolean parentgleaves[] = new boolean[++maxpleaves];
		for (VertexModel v : parentGraph) {
			if (Xi(v) != null)
				parentgleaves[Xi(v)] = true;
		}

		for (Triplet t : tow) {
			if ((t.i < maxsgleaves && sgleaves[t.i] && t.k < maxsgleaves && sgleaves[t.k]
					&& (t.j >= maxsgleaves || !sgleaves[t.j]) && t.j < maxpleaves && parentgleaves[t.j])

					||

					(t.j < maxsgleaves && sgleaves[t.j] && t.k < maxsgleaves && sgleaves[t.k]
							&& (t.i >= maxsgleaves || !sgleaves[t.i]) && t.i < maxpleaves && parentgleaves[t.i])) {
				return true;
			}

		}
		return false;

		// HashMap<Pair<Integer, Integer>, ArrayList<Triplet>> lookup = new
		// HashMap<Pair<Integer, Integer>, ArrayList<Triplet>>();
		// for (Triplet t : tow) {
		// if (sgleaves[t.i] && sgleaves[t.k]) {
		// Pair<Integer, Integer> k = new Pair<Integer, Integer>(t.i, t.k);
		// if (lookup.containsKey(k)) {
		// lookup.get(k).add(t);
		// } else {
		// ArrayList<Triplet> a = new ArrayList<Triplet>();
		// a.add(t);
		// lookup.put(k, a);
		// }
		// }
		// if (sgleaves[t.j] && sgleaves[t.k]) {
		// Pair<Integer, Integer> k = new Pair<Integer, Integer>(t.j, t.k);
		// if (lookup.containsKey(k)) {
		// lookup.get(k).add(t);
		// } else {
		// ArrayList<Triplet> a = new ArrayList<Triplet>();
		// a.add(t);
		// lookup.put(k, a);
		// }
		// }
		// }
		//		
		// for (int i : sg.leaves) {
		// for (int j : sg.leaves) {
		// if (!lookup.containsKey(new Pair(i, j)))
		// continue;
		// for (Triplet t : lookup.get(new Pair(i, j))) {
		// if ((t.i == i && t.k == j && !sgleaves[t.j] && parentgleaves[t.j])
		// || (t.j == i && t.k == j && !sgleaves[t.i] && parentgleaves[t.i])) {
		// // println(sg + ": " + t);
		// // if (verbose)
		// // println("nonsep" + vertices(sg) + "::" + t);
		// return true;
		// }
		// }
		// }
		// }
		// return false;
	}

	public static boolean isNonSeperable(LeafSet all, LeafSet sg, Vector<Triplet> tow) {
		for (int i : sg.leaves) {
			for (int j : sg.leaves) {
				if (i < j) {
					for (Triplet t : tow) {
						if (((t.i == i) && (t.k == j)&& (all.leaves
								.contains(Integer.valueOf(t.j))))
								|| ((t.j == i) && (t.k == j)
										&& (!sg.leaves.contains(Integer.valueOf(t.i))) && (all.leaves
										.contains(Integer.valueOf(t.i))))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	static void setVertexId(VertexModel v, Object id) {
		v.setUserDefinedAttribute("Xi", id);
	}

	/**
	 * @return The i of X this vertex is related to, or null if not ny one exist
	 */
	static Integer Xi(VertexModel vi) {
		return vi.getUserDefinedAttribute("Xi");
	}

	protected void dumpResultGraph() {
		// for (VertexModel v : network) {
		// println(v);
		// }
		// for (EdgeModel e : network.getEdges()) {
		// println(e.source.getLabel() + "->" + e.target.getLabel());
		// }
	}

	public static GraphModel graph(SubGraph sg) {
		GraphModel ret = new GraphModel(sg.graph.isDirected());
		for (VertexModel v : sg.vertices) {
			VertexModel vv = v.getCopy();
			v.getProp().obj = vv;
			ret.insertVertex(vv);
		}
		for (EdgeModel e : sg.edges) {
			EdgeModel ee = e.getCopy((VertexModel) e.source.getProp().obj, (VertexModel) e.target
					.getProp().obj);
			ret.insertEdge(ee);
		}

		return ret;
	}

	public static LeafSet leafSet(SubGraph sg) {
		LeafSet ls = new LeafSet();
		for (VertexModel v : OLDCODES.getVertices(sg)) {
			ls.leaves.add(Xi(v));
		}
		ls.sg = sg;
		return ls;
	}

	static HashMap<LeafSet, VertexModel> getLeafVerMap(GraphModel g) {
		if (g.getUserDefinedAttribute("leafver") == null) {
			g.setUserDefinedAttribute("leafver", new HashMap<LeafSet, VertexModel>());
		}
		return g.getUserDefinedAttribute("leafver");
	}

	static VertexModel vertex(int leaf, GraphModel g) {
		LeafSet a = leaf(leaf);
		return vertex(a, g);
	}

	private static LeafSet leaf(int leaf) {
		LeafSet a = new LeafSet();
		a.leaves.add(leaf);
		return a;
	}

	static VertexModel vertex(TripNetDS inp) {
		VertexModel vertexModel = vertex(inp.leafset, inp.network);
		setTripNetInstance(vertexModel, inp);
		return vertexModel;
	}

	/**
	 * Use this method when you do sure that a vertex having ls exists in graph
	 */
	static VertexModel searchForLeastVertexHaving(LeafSet ls, GraphModel g) {
		HashMap<LeafSet, VertexModel> leafVer = getLeafVerMap(g);
		if (leafVer.containsKey(ls))
			return leafVer.get(ls);
		else {
			// search for the leas vertex containing the
			int min = Integer.MAX_VALUE;
			VertexModel ret = null;
			for (VertexModel v : g) {
				LeafSet vls = getLeafSet(v);
				if (vls.leaves.containsAll(ls.leaves)) {
					if (min > vls.leaves.size()) {
						min = vls.leaves.size();
						ret = v;
					}
				}
			}
			return ret;
		}
	}

	static VertexModel vertex(LeafSet ls, GraphModel g) {
		HashMap<LeafSet, VertexModel> leafVer = getLeafVerMap(g);
		if (leafVer.containsKey(ls))
			return leafVer.get(ls);
		else {
			return createVertexAndPutInLeafSetBank(ls, g);
		}
	}

	static VertexModel createVertexAndPutInLeafSetBank(LeafSet ls, GraphModel g) {
		VertexModel v = createVertex(ls, g);
		getLeafVerMap(g).put(ls, v);
		return v;
	}

	static VertexModel createVertex(LeafSet ls, GraphModel g) {
		VertexModel v = new VertexModel();
		if (ls.leaves.size() == 1) {
			v.setUserDefinedAttribute("leave", true);
		}
		v.setUserDefinedAttribute("Leaf Set", ls);
		v.setLabel(ls.toString());
		g.insertVertex(v);
		return v;
	}

	public static void addToLeafSet(VertexModel v, LeafSet toAdd, GraphModel g) {
		LeafSet ls = getLeafSet(v).getACopy();
		if (ls == null)
			return;
		if (toAdd == null)
			return;
		HashMap<LeafSet, VertexModel> leafVer = getLeafVerMap(g);
		if (ls.contains(toAdd)) {
			if (leafVer.get(ls) == v)
				leafVer.put(ls, v);
			return;
		}
		if (leafVer.get(ls) == v)
			leafVer.remove(ls);
		ls.leaves.addAll(toAdd.leaves);
		v.setUserDefinedAttribute("Leaf Set", ls);
		v.setLabel(ls.toString());
		leafVer.put(ls, v);
		// add toAdd to parent of v and so on ...
		for (VertexModel p : g.getBackNeighbours(v)) {
			addToLeafSet(p, toAdd, g);
		}
		// for (Iterator<EdgeModel> ite = g.edgeIterator(); ite.hasNext();)
		// {//todo: zamane ejrasho behtar konam
		// EdgeModel e = ite.next();
		// if (e.target == v) {
		// addToLeafSet(e.source, toAdd, g);
		// }
		// }
	}

	// ---------------- CREATE TREE ----------------------------------

	// ------------------- AHO -------------------------

	static boolean isAhoDecomposable(TripNetDS inp) {
		if (inp.leafset.leaves.size() == 1) {
			return true;
		}

		GraphModel aho = new GraphModel(false);
		Vector<Triplet> T = Utils.getInducedTriplets(inp.tow, inp.leafset);
		HashMap<Object, VertexModel> vertices = new HashMap<Object, VertexModel>();

		for (Integer i : inp.leafset.leaves) {
			Utils.getVertex(i, aho, vertices);
		}
		// construct aho relation graph
		for (Triplet t : T) {
			aho
					.insertEdge(Utils.getVertex(t.i, aho, vertices), Utils.getVertex(t.j, aho,
							vertices));
		}
		// PreviewPluginMethods
		if (numOfComponents(aho) == 1) {
			return false;
		} else {
			return true;

		}
	}

	/**
	 * assumes ls to be tree just for one level and uses doAlgorithm(sg) to
	 * build its below
	 * 
	 * @return null if it is not a tree, else the tree root
	 */
	static VertexModel buildAhoTree_OneLevel(TripNetDS inp) {
		if (inp.leafset.leaves.size() == 1) {
			return vertex(inp.leafset, inp.network);
		}

		GraphModel aho = new GraphModel(false);
		Vector<Triplet> T = Utils.getInducedTriplets(inp.tow, inp.leafset);
		HashMap<Object, VertexModel> vertices = new HashMap<Object, VertexModel>();

		for (Integer i : inp.leafset.leaves) {
			Utils.getVertex(i, aho, vertices);
		}
		// construct aho relation graph
		for (Triplet t : T) {
			aho
					.insertEdge(Utils.getVertex(t.i, aho, vertices), Utils.getVertex(t.j, aho,
							vertices));
		}
		// PreviewPluginMethods
		if (numOfComponents(aho) == 1) {
			return null;
		} else {
			ArrayList<SubGraph> comps = getConnectedComponents(aho);
			if (comps.size() == 0) {
				throw new RuntimeException("0 Comps!");
			}
			Vector<LeafSet> factors = new Vector<LeafSet>();
			for (SubGraph sg : comps) {
				factors.add(leafSet(sg));
			}

			LeafSet right, left;

			if (comps.size() == 2) {
				right = factors.get(0);
				left = factors.get(1);
			} else {
				// find the component of aho with max weight to another comps
				// and put it in one side, and another comps in another side
				findMaxMinWeights(inp, factors); // todo: is this line necesary
				// here?
				right = factors.get(0);
				for (LeafSet scan : factors) {
					if (scan.maxw > right.maxw
							|| (scan.maxw == right.maxw && scan.maxwOcurrance > right.maxwOcurrance))
						right = scan;
				}
				left = factors.get(0).getACopy();
				if (left.equals(right))
					left = factors.get(1);
				for (LeafSet l : factors) {
					if (l != right)
						left.add(l);
				}
			}
			println("It's a Tree (AHO):" + right + "," + left);

			VertexModel v = vertex(inp.getsubinstance(right));
			VertexModel u = vertex(inp.getsubinstance(left));

			VertexModel root = vertex(inp.leafset, inp.network);

			insertEdge(root, v, inp.network);
			insertEdge(root, u, inp.network);
			return root;
		}
	}

	/**
	 * sets LeafSet.maxw, minw, maxwOccurance of leaf sets
	 * 
	 * @param leafSets
	 * @return pair.first : global maximum weight between leaves, pair.second:
	 *         minimum
	 */
	public static Pair<Integer, Integer> findMaxMinWeights(TripNetDS inp, Vector<LeafSet> leafSets) {
		int n = leafSets.size();
		int maxw = Integer.MIN_VALUE;
		int minw = Integer.MAX_VALUE;
		for (int i = 0; i < n; i++)
			for (int j = i + 1; j < n; j++) {
				Pair<Integer, Integer> p = inp.getWeight(leafSets.get(i), leafSets.get(j));
				int max = p.first, min = p.second;
				LeafSet lsi = leafSets.get(i);
				LeafSet lsj = leafSets.get(j);
				maxw = Math.max(maxw, max);
				minw = Math.min(minw, min);
				updateMaxW(lsi, max);
				updateMaxW(lsj, max);
				// lsi.maxw = Math.max(lsi.maxw, w);
				lsi.minw = Math.min(lsi.minw, min);
				// lsj.maxw = Math.max(lsj.maxw, w);
				lsj.minw = Math.min(lsj.minw, min);
				if (logLevel == 0)
					println(max + "," + min + "\t:" + lsi + "," + lsj);
			}
		return new Pair<Integer, Integer>(maxw, minw);

	}

	static void updateMaxW(LeafSet lsi, int w) {
		if (lsi.maxw < w) {
			lsi.maxw = w;
			lsi.maxwOcurrance = 1;
		} else if (lsi.maxw == w)
			lsi.maxwOcurrance++;
	}

	/**
	 * the triplets and ls will form a tree. the don't contain any of
	 * reticulations. then the method will add the reticulations inorder to
	 * create a simple network
	 * 
	 * @param reticulations
	 * @return
	 */
	static VertexModel addReticulationsToNetwork(TripNetDS startingTree,
			Vector<Reticulation> reticulations, TripNetDS completeInput) {
		for (Reticulation r : reticulations) {
			VertexModel reticulationRoot = vertex(startingTree.getsubinstance(r.reticulate));
			if (r.reticulate.leaves.size() > 1) {
				TripNetDS retins = completeInput.getsubinstance(r.reticulate);
				retins.network = startingTree.network;
				// doAlgorithm(retins);
				recurseAlgorithm(retins);
			}
			VertexModel reticV = createVertexAndPutInLeafSetBank(r.reticulate, startingTree.network);
			insertEdge(reticV, reticulationRoot, startingTree.network);
			insertReticulationVertex(r, startingTree);
		}
		return vertex(completeInput.leafset, startingTree.network);
	}

	/**
	 * inserts some edges to the given network, so that the network will have
	 * toadd triplets after.
	 * 
	 * @param network
	 * @param toAdd
	 */
	static void insertEdgesToAddTriplets(GraphModel network, Vector<Triplet> toAdd) {
		for (Triplet t : toAdd) {
			insertEdgeToAddTriplet(t, network);
		}
	}

	private static void insertEdgeToAddTriplet(Triplet t, GraphModel network) {
		// GraphModel U = Utils.createUndirectedCopy(network);
		// BiconnectedComponents bc = new BiconnectedComponents();
		// Vector<HashSet<VertexModel>> comps = bc.biconnected_components(U,
		// U.getAVertex(), U.getVerticesCount());

		if ((t + "").equals("1,8|5")) {
			System.err.println("1,5 9");
		}
		VertexModel _i = searchForLeastVertexHaving(leaf(t.i), network);
		VertexModel _j = searchForLeastVertexHaving(leaf(t.j), network);
		VertexModel _k = searchForLeastVertexHaving(leaf(t.k), network);

		// Vector<EdgeModel> hcasi = getAllHighestCutArcs_usingLeafSets(_i,
		// network, all, tow);
		// Vector<EdgeModel> hcasj = getAllHighestCutArcs_usingLeafSets(_j,
		// network, all, tow);
		// Vector<EdgeModel> hcask = getAllHighestCutArcs_usingLeafSets(_k,
		// network, all, tow);

		EdgeModel hcai = getHighestCutArc(_i, network);
		EdgeModel hcaj = getHighestCutArc(_j, network);
		EdgeModel hcak = getHighestCutArc(_k, network);
		VertexModel i = _i, j = _j;

		if (hcai != hcaj && hcai != hcak && hcaj != hcak && hcaj != null && hcai != null
				&& hcai.target != null && hcaj.target != null) {
			i = hcai.target;
			j = hcaj.target;
		} else {
			// Deque<VertexModel> jp = getParentsToRoot(_j, network);
			// Deque<VertexModel> kp = getParentsToRoot(_k, network);
			// Deque<VertexModel> ip = getParentsToRoot(_i, network);
			// Iterator<VertexModel> ji = jp.iterator();
			// while (kp.contains(j=ji.next()));
			// Iterator<VertexModel> ii = ip.iterator();
			// while (kp.contains(i=ii.next()));
		}

		// HashSet<VertexModel> compi = null, compj = null;
		// VertexModel i = _i, j = _j;
		// for (HashSet<VertexModel> comp : comps) {
		// if (comp.contains(_i.getProp().obj)) {
		// compi = comp;
		// }
		// if (comp.contains(_j.getProp().obj)) {
		// compj = comp;
		// }
		// }
		// if (compi == compj) {
		// i = _i;
		// j = _j;
		// } else {
		// VertexModel pi = _i;
		// while (compi.contains(pi)) {
		// i = pi;
		// pi = getParent(pi, network);
		// }
		// VertexModel pj = _j;
		// while (compi.contains(pj)) {
		// j = pj;
		// pj = getParent(pj, network);
		// }
		// }

		// if i or j are parent of a reticulation, the reticulation shouild be
		// considered
		if (network.getOutDegree(i) == 1) {
			i = network.getNeighbors(i).iterator().next();
			getLeafVerMap(network).put(leaf(t.i), i);
		}//
		if (network.getOutDegree(j) == 1) {
			j = network.getNeighbors(j).iterator().next();
			getLeafVerMap(network).put(leaf(t.j), j);
		}
		// conect father of i to father of j
		VertexModel pi = getParent(i, network);
		VertexModel pj = getParent(j, network);

		if (pi == null || i == null || j == null || pj == null) {
			println("ERRRrrrr");
		}
		EdgeModel ei = network.getEdge(pi, i);
		network.removeEdge(ei);
		VertexModel betweeni = createVertex(getLeafSet(i), network);
		insertEdge(pi, betweeni, network);
		insertEdge(betweeni, i, network);

		EdgeModel ej = network.getEdge(pj, j);
		network.removeEdge(ej);
		VertexModel betweenj = createVertex(getLeafSet(j), network);
		insertEdge(pj, betweenj, network);
		insertEdge(betweenj, j, network);
		// ----- And noW!
		insertEdge(betweeni, betweenj, network);
	}

	/**
	 * builds an Aho tree to the leaves in the given graph
	 * 
	 * @return null if it is not tree, else its root
	 */
	static VertexModel buildAhoTree(TripNetDS contractedInp) {
		if (contractedInp.leafset.leaves.size() == 1) { // todo: here needs a
			// better look, for
			// sparse cases
			return vertex(contractedInp.leafset, contractedInp.network);
			// return
			// vertex(inp.getsubinstance(inp.leafset.contractedFrom.get(contractedInp.leafset.leaves.iterator().next())));
		}
		GraphModel aho = new GraphModel(false);
		HashMap<Object, VertexModel> vertices = new HashMap<Object, VertexModel>();

		// make tow as T induced on ls
		Vector<Triplet> tow = Utils.getInducedTriplets(contractedInp.tow, contractedInp.leafset);

		// construct aho relation graph
		for (int i : contractedInp.leafset.leaves) {
			VertexModel vertex = Utils.getVertex(i, aho, vertices);// this
			// ensures
			// that all
			// leaves
			// have
			// vertices
		}
		for (Triplet t : tow) {
			aho
					.insertEdge(Utils.getVertex(t.i, aho, vertices), Utils.getVertex(t.j, aho,
							vertices));
		}

		if (numOfComponents(aho) == 1) {
			throw new RuntimeException("Err 5: triplets considered to be a tree but they aren't");
		} else {
			ArrayList<SubGraph> comps = getConnectedComponents(aho);
			TripNetDS inp2 = contractedInp
					.getsubinstance(leafSet(comps.get(0)));
			VertexModel v = buildAhoTree(inp2);

			SubGraph reminder;
			if (comps.size() == 2)
				reminder = comps.get(1);
			else {
				reminder = comps.get(1);
				for (int i = 2; i < comps.size(); i++) {
					reminder.add(comps.get(i));
				}
			}
			VertexModel u = buildAhoTree(contractedInp
					.getsubinstance(leafSet(reminder)));

			LeafSet rt = new LeafSet();
			rt.leaves.addAll(getLeafSet(u).leaves);
			rt.leaves.addAll(getLeafSet(v).leaves);
			VertexModel root = vertex(rt, contractedInp.network);

			insertEdge(root, v, contractedInp.network);
			insertEdge(root, u, contractedInp.network);
			return root;
		}
	}

	static LeafSet getLeafSet(SubGraph sg) {
		LeafSet ret = new LeafSet();
		for (VertexModel v : sg.vertices) {
			ret.leaves.add((Integer) (Xi(v)));
		}
		ret.sg = sg;
		return ret;
	}

	static void insertReticulationVertex(Reticulation r, TripNetDS inp) {
		VertexModel rightChild = r.rightChild != null ? searchForLeastVertexHaving(
				r.rightChild, inp.network)
				: null;
		VertexModel leftChild = r.leftChild != null ? searchForLeastVertexHaving(
				r.leftChild, inp.network)
				: null;
		VertexModel reticv = vertex(r.reticulate, inp.network);
		if (rightChild == null || leftChild == null) {
			VertexModel child = rightChild != null ? rightChild : leftChild;
			LeafSet l = new LeafSet();
			l.leaves.addAll(getLeafSet(child).leaves);
			l.leaves.addAll(r.reticulate.leaves);
			if (l.equals(inp.leafset)) {
				if (inp.network.getOutDegree(child) != 1) {
					VertexModel t = new VertexModel(); // todo: t has no leafSet
					// assigned to it! is
					// there a problem here?
					inp.network.insertVertex(t);
					insertEdge(t, child, inp.network);
					insertEdge(t, reticv, inp.network);
					VertexModel root = vertex(inp.leafset, inp.network);
					insertEdge(root, reticv, inp.network);
					insertEdge(root, t, inp.network);
				} else
					throw new RuntimeException("Err #6: Child outdegree is 1");
			} else {
				insertReticV(child, reticv, inp.network);
				LeafSet rootls = inp.leafset;
				rootls.leaves.addAll(r.reticulate.leaves);
				VertexModel root = getRoot(inp);
				// VertexModel root = vertex(inp.leafset, inp.network);
				insertEdge(root, reticv, inp.network);
			}
		} else {
			insertReticV(rightChild, reticv, inp.network);
			insertReticV(leftChild, reticv, inp.network);
		}

	}

	static void insertReticV(VertexModel child, VertexModel reticV, GraphModel graph) {
		if (graph.getOutDegree(child) == 1) {
			child = graph.getNeighbors(child).iterator().next();
		}
		VertexModel parent = getParent(child, graph);
		EdgeModel e = graph.getEdge(parent, child);
		graph.removeEdge(e);
		VertexModel t = vertex(LeafSet.union(getLeafSet(child), getLeafSet(reticV)), graph); // todo: set the leafset
		if (t == parent) {
			t = createVertexAndPutInLeafSetBank(getLeafSet(t), graph);
		}
		insertEdge(t, child, graph);
		insertEdge(t, reticV, graph);
		insertEdge(parent, t, graph);
		addToLeafSet(t, getLeafSet(reticV), graph); // !nabaiad comment beshe
	}

	static void insertEdge(VertexModel src, VertexModel trg, GraphModel graph) {
		if (src == trg)
			return;
		if (graph.getInDegree(trg) == 2)
			throw new RuntimeException("target indegree is larger than 2");

		VertexModel p;
		if ((graph.getInDegree(src) == 2) && (graph.getOutDegree(src) == 1)) {
			VertexModel c = (VertexModel) graph.getNeighbors(src).iterator().next();
			LeafSet ls = getLeafSet(c);
			ls.add(getLeafSet(trg));
			p = createVertexAndPutInLeafSetBank(ls, graph);
			graph.removeAllEdges(src, c);
			graph.insertEdge(src, p);
			graph.insertEdge(p, c);
			graph.insertEdge(p, trg);
			return;
		}
		if (graph.getOutDegree(src) == 2) { // todo: check for if inputDegree of
			// v2 ==2?
			VertexModel u = createVertexAndPutInLeafSetBank(getLeafSet(src),
					graph);
			for (VertexModel parent : graph) {
				if (graph.isEdge(parent, src)) {
					graph.removeEdge(graph.getEdge(parent, src));
					graph.insertEdge(new EdgeModel(parent, u));
				}
			}
			graph.insertEdge(new EdgeModel(u, src));
			graph.insertEdge(new EdgeModel(u, trg));
			addToLeafSet(u, getLeafSet(trg), graph);
		} else {
			graph.insertEdge(new EdgeModel(src, trg));
			addToLeafSet(src, getLeafSet(trg), graph);
		}
	}

	static VertexModel getParent(VertexModel child, GraphModel graph) {
		Iterator<VertexModel> it = graph.getBackNeighbours(child).iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

	public static TripNetDS getTripNetInstance(VertexModel v) {
		return v.getUserDefinedAttribute("TripNetDS");
	}

	public static void setTripNetInstance(VertexModel v, TripNetDS inp) {
		v.setUserDefinedAttribute("TripNetDS", inp);
	}

	public static LeafSet getLeafSet(VertexModel v) {
		LeafSet leafSet = v.getUserDefinedAttribute("Leaf Set");
		if (leafSet == null) {
			Integer x = Xi(v);
			if (x != null) {
				leafSet = new LeafSet();
				leafSet.leaves.add(x);
			}
		}
		return leafSet;
	}

	public static Pair<Vector<LeafSet>, Vector<LeafSet>> factorize(TripNetDS inp, boolean tryToUnify) {
		println("Factorizing " + inp.leafset.leaves.size() + " nodes : " + inp.leafset);
		Vector<LeafSet> allFactors = new Vector<LeafSet>();
		Vector<LeafSet> lowestFactors = new Vector<LeafSet>();
		GraphModel g = inp.createFGraph();
		// GraphData gd = new GraphData(Application.blackboard);
		// gd.core.showGraph(g);
		SubGraph last = null; // todo: in case of NullPointerException, the last
		// should be initialized with the whole graph as
		// a subgraph
		System.out.println("removing max weight in graph:");
		if (inp.leafset.leaves.size() != 1) {
			while (numOfComponents(g) == 1) {
				int w = getMaxWeight(g);
				removeEdgesWithWeight(w, g);
				System.out.print(", " + w);
			}
			ArrayList<SubGraph> nonSep;
			boolean lastSet = true;
			System.out.println("removing max weight in subgraphs");
			while ((nonSep = getNonSepComps(g, inp.tow)).size() > 0) {
				if (nonSep.size() == 1 && !lastSet) {
					last = nonSep.get(0);
					lastSet = true;
				}
				if (nonSep.size() > 1)
					lastSet = false;
				System.out.print(".");
				for (SubGraph sg : nonSep) {
					removeMaxWeightedEdges(sg);
				}
			}
		}
		HashSet<Integer> lastleaves = last == null ? inp.leafset.leaves
				: getLeafSet(last).leaves;
		for (SubGraph sg : getConnectedComponents(g)) {
			LeafSet ls = leafSet(sg);
			allFactors.add(ls);
			if (lastleaves.containsAll(ls.leaves)) {
				lowestFactors.add(ls);
			}
		}
		// if (tryToUnify) {
		// Vector<LeafSet> unifiedAll = new Vector<LeafSet>();
		// for (LeafSet ls : allFactors) {
		// if (!ls.mark) {
		// ls.mark = true;
		// LeafSet unifiedls = ls.getACopy();
		// ls.unifiedFactorizationTarget = unifiedls;
		// for (LeafSet ls2 : allFactors) {
		// if (ls2.mark)
		// continue;
		// LeafSet candid = unifiedls.getACopy();
		// candid.add(ls2);
		// if (!isNonSeperable(inp.leafset, candid, inp.tow)){
		// unifiedls.add(ls2);
		// ls2.unifiedFactorizationTarget = unifiedls;
		// ls2.mark = true;
		// }
		// }
		// unifiedAll.add(unifiedls);
		// } //end if (!ls.mark)
		// }
		// Vector<LeafSet> unifiedLowest = new Vector<LeafSet>();
		// HashSet<LeafSet> hs = new HashSet<LeafSet>();
		// for (LeafSet ls:lowestFactors) {
		// unifiedLowest.add(ls.unifiedFactorizationTarget);
		// }
		// return new Pair<Vector<LeafSet>, Vector<LeafSet>>(unifiedAll,
		// unifiedLowest);
		// }
		return new Pair<Vector<LeafSet>, Vector<LeafSet>>(allFactors,
				lowestFactors);
		// return new Pair<Vector<LeafSet>,
		// Vector<LeafSet>>(extendLeafSets(allFactors),
		// extendLeafSets(lowestFactors));
	}

	static JTextArea t;

	public static void println(Object o) {
		if (!verbose)
			return;
		String prn = "";
		for (int i = 0; i < algDepth; i++)
			prn += "    ";
		System.out.println(prn + o);
		// add the text to the text box
		if (t != null) {
			String s = t.getText();
			String t1 = s + prn + o + "\n";
			t1 = t1.substring(t1.indexOf("\n", t1.length() - 3000));
			t.setText(t1);
			t.setCaretPosition(t.getText().length());
			if (log != null) {
				log.println(prn + o);
				log.flush();
			}
		}
		if (gui == null && log != null)
			EventUtils.algorithmStep(ths, prn + o);
	}

	public static void printCollection(Iterable o) {
		if (!verbose)
			return;
		String prn = "";
		for (int i = 0; i < algDepth; i++)
			prn += "\t";
		for (Object _ : o)
			prn = prn + (_ + ", ");
		println(prn);
		EventUtils.algorithmStep(ths, prn);
	}

	@SuppressWarnings( { "HardcodedLineSeparator" })
	public static String printDot(GraphModel result) {
		String ret = "";
		ret += "strict digraph G {\n";
		for (VertexModel v : result) {
			if (result.getOutDegree(v) > 0)
				// ret += MessageFormat.format("{0} [shape=circle];\n",
				// getVertexId(v));
				ret += MessageFormat.format(
						"{0} [shape=point, fontsize=\"0\"];\n", getVertexId(
								result, v));
			else
				ret += MessageFormat.format("{0} [shape=circle];\n",
						getVertexId(result, v));
		}
		for (EdgeModel e : result.edges()) {
			ret += MessageFormat.format("{0} -> {1} [arrowsize=\".8\"];\n",
					getVertexId(result, e.source),
					getVertexId(result, e.target));
		}
		ret += "}";
		return ret;
	}

	public static String getVertexId(GraphModel result, VertexModel v) {
		HashSet<Integer> lvs = getLeafSet(v).leaves;
		if (result.getOutDegree(v) == 0) {
			String id = null;
			try {
				if (lvs.size() > 1) {
					id = "\"" + lvs + "\"";
				} else
					id = lvs.iterator().next() + "";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return id + "";
			// return v.getId() + "00" +id;
		} else
			return v.getId() + "." + lvs.size();
	}

	static PrintWriter log;

	public static void main(String[] args) {
		System.out.println("TripNet 1.1");
		if (args.length > 0) {
			if ((args.length > 1) && (args[1] != null)) {
				List a = Arrays.asList(args);
				verbose = a.contains("-v");
				if (a.contains("-speed")){
					int i = a.indexOf("-speed");
					if (a.get(i+1).equals("fast")){
						TripNet.speed=SPEED_FAST;
					}
					if (a.get(i+1).equals("normal")){
						TripNet.speed = SPEED_NORMAL;
					}
					if (a.get(i+1).equals("slow")){
						TripNet.speed = SPEED_SLOW;
					}
				}
//				REAMOVE_CYCLIC_TRIPLETS = !a.contains("-notexact");
//				if (a.contains("-minw")) {
//					int i = a.indexOf("-minw");
//					TRIPLET_WEIGHT_MIN_BOUND = Double.parseDouble((String) a.get(i + 1));
//				}
			}

			if (args[0] != null) {
				tripletsFile = new File(args[0]);
				if (!tripletsFile.exists()) {
				    println(args[0]);
					println("The given file does not exist!");
					println(tripletsFile);
					return;
				}
				String logpath = tripletsFile.getAbsolutePath() + ".log";
				try {
					log = new PrintWriter(logpath);
				} catch (FileNotFoundException e) {
					StaticUtils.addExceptionLog(e);
				}
				TripNet t = new TripNet();
				t.doAlgorithm();
				log.close();
				println("You may see a complete log of algorithm run  on: "
						+ logpath);
				return;
			}
		}
		System.out.println("usage: TripNet file_name [options]");
		System.out.println("file_name is the path of triplets file.");
		System.out.println("options:");
		System.out.println("-v \t Verbose: prints information about how the algorithm runs.");
		System.out.println("-speed \t (fast | normal | slow): determines the speed of algorithm, using a " +
				"faster speed will cause your network to have a higher level and in a slower speed, the program" +
				"will try to find networks with a lower network by checking more possibilities.");
		
		System.out.println("example: java -cp \"../lib/*:.\"  phylogenetic.Main \"my triplets.txt\" -v ");
		//		System.out.println("-notexact \t Not Exact: The algorithm may miss some triplets because of conflicts in input triplets");
//		System.out.println("-minw weight \t Minimum Weight: used when a weights file exists beside triplets file. the name of the weight file should be the same as triplets file + -weights. for example triplets.txt and triplets.txt - weight . in this case each line of the weights file contains a double number which is the weights of the same line triplet in triplets file, so these two files must have the same number of lines. having a weights file affects TripNet in two ways: 1- finding a conflict cycle in input triplets it will removes the triplet in the	cycle with minimum weight, 2- defining minw, it will discard all triplets with weight less than minw.");
	}

	static class ReticulationInstance {
		TripNetDS reminder;
		Vector<Reticulation> reticulations;
		Vector<LeafSet> lowestFactors;

		public int hashCode() {
			return reticulations.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof ReticulationInstance) {
				ReticulationInstance reticulationInstance = (ReticulationInstance) obj;
				return reticulations.equals(reticulationInstance.reticulations);
			}
			return false;
		}

		public ReticulationInstance() {
		}

		public ReticulationInstance(TripNetDS reminder, Vector<Reticulation> reticulations,
				Vector<LeafSet> lowestFactors) {
			this.reminder = reminder;
			this.reticulations = reticulations;
			this.lowestFactors = lowestFactors;
		}
	}

	/**
	 * runs after a reticulation selection found, it has a minFoundRetics which
	 * helps the reticulation selection finding to have an alpha cut searching,
	 * which means that, do not seek reticulation selections which have more
	 * than minFoundRetics, reticulations
	 */
	abstract static class RunOnReticulationSelectionFound {
		int minFoundRetics = Integer.MAX_VALUE;
		TripNetDS min;

		abstract void reticulationSelectionFound(Pair<TripNetDS, Vector<Reticulation>> rs);

		abstract void finished();
	}
}
