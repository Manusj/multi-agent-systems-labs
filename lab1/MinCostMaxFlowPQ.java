// --------------------------------
// TDDE13 - Multi-agent systems lab
// --------------------------------
// Base classes and input/output handling for Task 1.
// Your implementation goes inside the "main" function.
// You should not have to do any implementations elsewhere to pass 
// this part of the lab.
// Good luck!

import java.io.*;
import java.util.*;

/**
 * An implementation of Edmond Karp's algorithm using adjecency list and edge
 * cost. Time complexity O(Vlog(V)E^2), which in the domain of assignment
 * problem becomes O(N^5logN).
 */
public class MinCostMaxFlowPQ {
	private MinCostMaxFlowPQ() {
	}

	private static class Edge {
		final int u, v, cost;
		int flow, capacity;
		Edge backEdge;

		Edge(final int u, final int v, final int capacity, final int cost) {
			this.u = u;
			this.v = v;
			this.flow = 0;
			this.capacity = capacity;
			this.cost = cost;
		}

		// Adds a given flow to the edge.
		void addFlow(int flow) {
			this.flow += flow;
			backEdge.capacity += flow;
		}

		// Returns the nodes capacity.
		int getNetCapacity() {
			return capacity - flow;
		}

		int getFlow() {
			return this.flow - backEdge.flow;
		}

		@Override
		public String toString() {
			return String.format("%d %d %d", u, v, getFlow());
			//return String.format("%d -> %d (%d)", u, v, flow);
		}
	}

	private static List<List<Edge>> graph;
	private static final int INF = 1_000_000_000;

	/**
	 * Computes the maximum flow from the source to the sink using Edmond Karp's
	 * algorithm. Time complexity is O(Vlog(V)E^2). Flow information is saved
	 * directly in the graph representation.
	 *
	 * @param source The source vertex.
	 * @param sink   The sink vertex.
	 * @return The maxmium flow.
	 */
	private static int maxFlow(int source, int sink) {
		int maxFlow = 0;
		while (true) {
			List<Edge> path = findAugmentingPath(source, sink);
			if (path.isEmpty()) {
				break;
			}
			int f = minFlow(path);
			augment(path, f);
			maxFlow += f;
		}
		return maxFlow;
	}

	/**
	 * Returns the minimum available flow of the edges on the path.
	 *
	 * @param path A list of Edges representing the path.
	 * @return The minimum available flow found.
	 */
	private static int minFlow(List<Edge> path) {
		int min = Integer.MAX_VALUE;
		for (Edge edge : path) {
			min = Math.min(min, edge.getNetCapacity());
		}
		return min;
	}

	private static int getTotalCost() {
		int totalCost = 0;
		for (List<Edge> edges : graph) {
			for (Edge edge : edges) {
				totalCost += edge.flow * edge.cost;
			}
		}
		return totalCost;
	}

	/**
	 * Adds flow to every Edge on the path and increases backedge capacity
	 * accordingly.
	 *
	 * @param path    The edges to augment.
	 * @param minFlow The flow to add.
	 */
	private static void augment(List<Edge> path, int minFlow) {
		for (Edge edge : path) {
			edge.addFlow(minFlow);
		}
	}

	/**
	 * Finds the cheapest path with unused capacity from the source to the sink.
	 *
	 * @param source The source.
	 * @param sink   The sink.
	 * @return The shortest path with unused capacity. If no path is found, the
	 *         returned list is empty.
	 */
	private static List<Edge> findAugmentingPath(int source, int sink) {
		int[] cost = new int[graph.size()];
		Edge[] parent = new Edge[graph.size()];
		Arrays.fill(cost, INF);

		// Queue of paths to examine. Each path is represented as a single "edge", where
		// cost is the total path cost.
		PriorityQueue<Edge> q = new PriorityQueue<>(new Comparator<Edge>() {
			@Override
			public int compare(final Edge o1, final Edge o2) {
				return o1.cost == o2.cost ? Integer.compare(o1.v, o2.v) : Integer.compare(o1.cost, o2.cost);
			}
		});

		q.add(new Edge(source, source, 0, 0));
		cost[source] = 0;

		// Run Dijkstra.
		while (!q.isEmpty()) {
			final Edge e = q.poll();
			final int u = e.v;
			if (e.cost > cost[u]) {
				continue;
			}

			for (Edge edge : graph.get(u)) {
				if (cost[u] + edge.cost < cost[edge.v] && edge.getNetCapacity() > 0) {
					parent[edge.v] = edge;
					cost[edge.v] = cost[u] + edge.cost;
					q.add(new Edge(u, edge.v, 0, cost[edge.v]));
				}
			}
		}

		// Backtrack path.
		List<Edge> res = new ArrayList<>();
		while (parent[sink] != null) {
			res.add(parent[sink]);
			sink = parent[sink].u;
		}

		return res;
	}

	/**
	 * Recreates the shortest path from an array of parents.
	 *
	 * @param parent      An array containing the parent id in the shortest path
	 *                    from the source to each node.
	 * @param destination The destination node.
	 * @return A list containing every node on the shortest path to the destination.
	 */
	private static List<Integer> recreatePath(int[] parent, int destination) {
		List<Integer> path = new ArrayList<>();
		while (parent[destination] != destination) {
			path.add(destination);
			destination = parent[destination];
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * Returns a list of all edges with effective flow, that is flow that is not
	 * cancelled out by any backflow.
	 *
	 * @return A list of all edges with effective flow.
	 */
	private static List<Edge> recreateFlowPath() {
		List<Edge> res = new ArrayList<>();
		for (List<Edge> edges : graph) {
			for (Edge edge : edges) {
				if (edge.getFlow() > 0) {
					res.add(edge);
				}
			}
		}
		return res;
	}

	/**
	 * Returns a list of the vertices in the S-component after a minumum cut. The
	 * minimum cut cost is the same as the maximum flow. Any egde going from the
	 * S-component to the T-component is part of the cut-set.
	 *
	 * This implementation assumes that maximum flow has already been calculated,
	 * and that the global graph thus contains correct flow information. If this is
	 * not the case, the behaviour is not defined.
	 *
	 * @param source The source vertice.
	 * @return A list of all vartices in the S-component.
	 */
	private static List<Integer> minCut(int source) {
		List<Integer> res = new ArrayList<>();
		boolean[] visited = new boolean[graph.size()];
		Queue<Integer> q = new LinkedList<>();
		visited[source] = true;
		q.add(source);
		res.add(source);

		while (!q.isEmpty()) {
			int u = q.poll();
			for (Edge edge : graph.get(u)) {
				if (!visited[edge.v] && edge.getNetCapacity() > 0) {
					visited[edge.v] = true;
					q.add(edge.v);
					res.add(edge.v);
				}
			}
		}

		return res;
	}

	public static void main(String[] args) {
		try (Scanner input = new Scanner(System.in)) {
			//System.out.println("\n\nStarting main"); 
			//System.out.println("Enter input size:"); 
			// Read the problem's input size.
			int n = input.nextInt();

			// Setup the graph!
			// Recall that it should have n + n + 1 + 1 = 2n + 2 vertices in total:
			// - n for the jobs,
			// - n for the agents,
			// - 1 for the source, and
			// - 1 for the sink.
			// We use 2n as the source index, and 2n + 1 as the sink index, but you are free
			// to
			// make a change if you wish (not required though).
			int[][] c = new int[n][n];
			graph = new ArrayList<>();
			for (int i = 0; i < 2 * n + 2; ++i) {
				graph.add(new ArrayList<>());
			}
			final int sourceVertexIndex = 2 * n;
			final int sinkVertexIndex = 2 * n + 1;

			// You should set up the flow graph correctly here.
			// Do not forget to add back edges accordingly.
			for (int i = 0; i < n; ++i) {
				
				// agent to tasks
				for (int j = 0; j < n; ++j) {
					int cost = input.nextInt(); // Read a single cost from the input.
					
					// Add edges and back edges ...
					Edge agentToTaskEdge = new Edge(i, j+n, 1, cost);
					agentToTaskEdge.backEdge = new Edge(j+n, i, 0, -cost);
					graph.get(i).add(agentToTaskEdge);
					graph.get(j+n).add(agentToTaskEdge.backEdge);
					//agentToTaskEdge.backEdge.backEdge = new Edge(i, j+n, 1, cost);
					agentToTaskEdge.backEdge.backEdge = agentToTaskEdge;
				}

				// source to agent
				Edge sourceToAgentEdge = new Edge(sourceVertexIndex, i, 1, 0);
				sourceToAgentEdge.backEdge = new Edge(i, sourceVertexIndex, 0, 0);
				graph.get(sourceVertexIndex).add(sourceToAgentEdge);
				graph.get(i).add(sourceToAgentEdge.backEdge);
				//sourceToAgentEdge.backEdge.backEdge = new Edge(sourceVertexIndex, i, 1, 0);
				sourceToAgentEdge.backEdge.backEdge = sourceToAgentEdge;


				// task to sink
				Edge taskToSinkEdge = new Edge(i+n, sinkVertexIndex, 1, 0);
				taskToSinkEdge.backEdge = new Edge(sinkVertexIndex, i+n, 0, 0);
				graph.get(i+n).add(taskToSinkEdge);
				graph.get(sinkVertexIndex).add(taskToSinkEdge.backEdge);
				//taskToSinkEdge.backEdge.backEdge = new Edge(i+n, sinkVertexIndex, 1, 0);
				taskToSinkEdge.backEdge.backEdge = taskToSinkEdge;
			}

			
			// Remember to also add edges and back edges from the source/sink vertices.
			
			
			// Find the graphs minimum cost maximum flow.
			int f = maxFlow(sourceVertexIndex, sinkVertexIndex);
			


			// Compute the total cost and print it.
			// Note that you might have to change how the vertices' indices are handled
			// depending
			// on how you set up your flow graph's vertices, edges and back edges.
			int totalCost = getTotalCost();
			System.out.println(totalCost);
			for (int i = 0; i < n; ++i) {
				int match = -1;
				for (int j = 0; j < n; ++j) {
					if (graph.get(i).get(j).getFlow() > 0) {
						match = j;
						break;
					}
				}
				if (i != 0) {
					System.out.print(" ");
				}
				System.out.print(match + 1);
			}
			System.out.println();
		}
	}

	/**
	 * Simple yet moderately fast I/O routines.
	 * <p>
	 * Example usage:
	 * <p>
	 * Kattio io = new Kattio(System.in, System.out);
	 * <p>
	 * while (io.hasMoreTokens()) { int n = io.getInt(); double d = io.getDouble();
	 * double ans = d*n;
	 * <p>
	 * io.println("Answer: " + ans); }
	 * <p>
	 * io.close();
	 * <p>
	 * <p>
	 * Some notes:
	 * <p>
	 * - When done, you should always do io.close() or io.flush() on the
	 * Kattio-instance, otherwise, you may lose output.
	 * <p>
	 * - The getInt(), getDouble(), and getLong() methods will throw an exception if
	 * there is no more data in the input, so it is generally a good idea to use
	 * hasMoreTokens() to check for end-of-file.
	 *
	 * @author: Kattis
	 */
	static class Kattio extends PrintWriter {
		public Kattio(InputStream i) {
			super(new BufferedOutputStream(System.out));
			r = new BufferedReader(new InputStreamReader(i));
		}

		public Kattio(InputStream i, OutputStream o) {
			super(new BufferedOutputStream(o));
			r = new BufferedReader(new InputStreamReader(i));
		}

		public boolean hasMoreTokens() {
			return peekToken() != null;
		}

		public int getInt() {
			return Integer.parseInt(nextToken());
		}

		public double getDouble() {
			return Double.parseDouble(nextToken());
		}

		public long getLong() {
			return Long.parseLong(nextToken());
		}

		public String getWord() {
			return nextToken();
		}

		private BufferedReader r;
		private String line;
		private StringTokenizer st;
		private String token;

		private String peekToken() {
			if (token == null)
				try {
					while (st == null || !st.hasMoreTokens()) {
						line = r.readLine();
						if (line == null)
							return null;
						st = new StringTokenizer(line);
					}
					token = st.nextToken();
				} catch (IOException e) {
				}
			return token;
		}

		private String nextToken() {
			String ans = peekToken();
			token = null;
			return ans;
		}
	}
}
