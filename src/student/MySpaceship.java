package student;

import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import controllers.Spaceship;
import models.Edge;
import models.Node;
import models.NodeStatus;

import controllers.SearchPhase;
import controllers.RescuePhase;

/** An instance implements the methods needed to complete the mission. */
public class MySpaceship implements Spaceship {
	
	/* planet IDs that have been visited in search algorithm */
	private ArrayList<Integer> planets; 
	/* Nodes that have been visited in rescue algorithm */
	private ArrayList<Node> collected; 
	/* Nodes that have been visited in rescue algorithm */
	private HashMap<Edge, Integer> banned; 
	
	/** Constructor: initializes fields */
	public MySpaceship() {
		planets = new ArrayList<Integer>();
		collected = new ArrayList<Node>();
		banned = new HashMap<Edge, Integer>();
	}
	
	/** The spaceship is on the location given by parameter state.
	 * Move the spaceship to Planet X and then return (with the spaceship is on
	 * Planet X). This completes the first phase of the mission.
	 * 
	 * If the spaceship continues to move after reaching Planet X, rather than
	 * returning, it will not count. If you return from this procedure while
	 * not on Planet X, it will count as a failure.
	 *
	 * There is no limit to how many steps you can take, but your score is
	 * directly related to how long it takes you to find Planet X.
	 *
	 * At every step, you know only the current planet's ID, the IDs of
	 * neighboring planets, and the strength of the signal from Planet X at
	 * each planet.
	 *
	 * In this rescuePhase,
	 * (1) In order to get information about the current state, use functions
	 * currentID(), neighbors(), and signal().
	 *
	 * (2) Use method onPlanetX() to know if you are on Planet X.
	 *
	 * (3) Use method moveTo(int id) to move to a neighboring planet with the
	 * given ID. Doing this will change state to reflect your new position.
	 */
	@Override
	public void search(SearchPhase state) {
		// TODO: Find the missing spaceship
		searchOptimized(state);
	}
	
	/** A DFS walk to search for Planet X */
	public void searchOptimized(SearchPhase state) {
		int start = state.currentID();
		planets.add(start);
		if (state.onPlanetX()) return;
		NodeStatus[] neighbors = optimizeHelper(state); 
		
		for (NodeStatus ns : neighbors) {
			int nsID = ns.id();
			
			if (planets.indexOf(nsID) == -1) {
				state.moveTo(nsID); 
				searchOptimized(state);
				
				if (state.onPlanetX()) return;
				state.moveTo(start);
			}
		}
	}
	
	/** Returns a sorted array in descending order of NodeStatus objects 
	 * based on signal strength of each NodeStatus object */
	public NodeStatus[] optimizeHelper(SearchPhase state) {
		NodeStatus neighbors[] = state.neighbors();
		int len = neighbors.length;
		
		for (int i = 0; i < len; i++) {
			int k = i;
			
			for (int j = i + 1; j < len; j++) {
				Double ksig = neighbors[k].signal();
				Double jsig = neighbors[j].signal();
				
				if (ksig.compareTo(jsig) < 0) k = j;
				
			NodeStatus t = neighbors[i];
			neighbors[i] = neighbors[k];
			neighbors[k] = t;
			}
		}
		return neighbors;
		
	} 
	
	/** The spaceship is on the location given by state. Get back to Earth
	 * without running out of fuel and return while on Earth. Your ship can
	 * determine how much fuel it has left via method fuelRemaining().
	 * 
	 * In addition, each Planet has some gems. Passing over a Planet will
	 * automatically collect any gems it carries, which will increase your
	 * score; your objective is to return to earth successfully with as many
	 * gems as possible.
	 * 
	 * You now have access to the entire underlying graph, which can be accessed
	 * through parameter state. Functions currentNode() and earth() return Node
	 * objects of interest, and nodes() returns a collection of all nodes on the
	 * graph.
	 *
	 * Note: Use moveTo() to move to a destination node adjacent to your current
	 * node. 
	 * 
	//look at neighbors, see which has more nodes, go to that, have helper that checks if enough fuel to get back
	//Make sure to check if that node is not in the shortest path back, another helper. call in rescue continues to return 
	//once we determine there is not enough fuel.
	 * 
	 */
	@Override
	public void rescue(RescuePhase state) {
		// TODO: Complete the rescue mission and collect gems
		gemCollection2(state);
		List<Node> path = pathToEarth(state);
		path.remove(0);
		for (Node st : path) {
			state.moveTo(st);
		}
	}
	
	/** A modified DFS walk to traverse a path to collect as many 
	 * gems as possible */
	public void gemCollection2(RescuePhase state) {
		Node n = state.currentNode(); 
		collected.add(n);
		HashMap<Node, Integer> map = n.neighbors();
		int mapLen = map.size();
		Node[] neighbors = removeBanned(
				sortGems(map.keySet().toArray(new Node[mapLen])), state);
		int len = neighbors.length;
		boolean check = false;
		
		for (int i = 0; i < len; i++) {
			Node target = neighbors[i];
			
			if (!fuelToReturn(target, state)) {
				
				if (i == len - 1) break;
				else if (fuelToReturn(neighbors[i + 1], state)) {
					target = neighbors[i + 1];
				} else break;
			}
			
			if (collected.indexOf(target) == -1) {
				state.moveTo(target); 
				check = true;
				break; 
			}
		}
		if (!check) {
			int next = index(neighbors, state);
			
			if (next == -1) {
				List<Node> path = optimalPath(state);
				if (path == null || path.size() == 0) return;
				for (Node x : path) {
					state.moveTo(x);
				}
			}
			else {
				Node back = neighbors[next];
				if (!fuelToReturn(back, state)) return; 
				state.moveTo(back);
				banned.put(n.getEdge(back), 
						Paths.pathWeight(Paths.minPath(n, back)));
			}
		}
		gemCollection2(state);
	}
	
	/** Returns true if the ship has enough fuel to return on the 
	 * shortest path from this node to earth; false otherwise */
	public boolean fuelToReturn(Node n, RescuePhase state) {
		Node prev = state.currentNode();
		Edge e = n.getEdge(prev);
		double len = 0;
		
		if (e != null) len = Paths.pathWeight(Paths.minPath(prev, n)) * 2;		
		List<Node> path = pathToEarth(state);
		int fuel = state.fuelRemaining();
		double required = Paths.pathWeight(path) + len;
		return fuel >= required;  
	}
	
	/** Returns a number that correlates to the index of a node that 
	 * has not been backtracked to already; returns -1 if there no
	 * such number */
	public int index(Node[] neighbors, RescuePhase state) {
		int len = neighbors.length;
		if (len <= 1) return 0;
		
		for (int i = 0; i < len; i++) {
			Node current = state.currentNode();
			Node n = neighbors[i];
			
			if (!banned.containsKey(current.getEdge(n))) {
				return i;
			}
		}
		return -1;
	}
	 
	/** Return a list containing the shortest path from the given 
	 * state to earth */
	public List<Node> pathToEarth(RescuePhase state) {
		Node start = state.currentNode();
		Node end = state.earth();
		return Paths.minPath(start, end);
	}
	
	/** Returns a sorted array of Nodes in descending order based
	 * on the number of gems each Node has */
	public Node[] sortGems(Node[] arr) {
		Node[] neighbors = arr;
		int len = neighbors.length;
		
		for (int i = 0; i < len; i++) {
			int k = i;
			
			for (int j = i + 1; j < len; j++) {
				Node knode = neighbors[k];
				Node jnode = neighbors[j];
				Integer kratio = knode.gems(); 
				Integer jratio = jnode.gems();
				
				if (kratio.compareTo(jratio) < 0) k = j;
			}
			Node t = neighbors[i];
			neighbors[i] = neighbors[k];
			neighbors[k] = t;
		} 
		return neighbors;
	}
	
	/** Returns a copy of the array that removes a node if it is 
	 * connected to this state by an Edge stored in the banned map; 
	 * returns original array if all Nodes are connected to this 
	 * state by to a banned Edge  */
	public Node[] removeBanned(Node[] arr, RescuePhase state) {
		ArrayList<Node> copy = new ArrayList<Node>();
		Node current = state.currentNode();
		
		for (Node n : arr) {
			if (!banned.containsKey(current.getEdge(n))) copy.add(n);
		}
		if (copy.size() == 0) return arr;
		return copy.toArray(new Node[copy.size()]);
	}
	
	/** Returns an array of remaining Nodes in the graph in descending 
	 * order based on the number of gems each node has. The number of 
	 * Nodes in the returned array is equivalent to roughly one tenth of
	 * the number of Nodes left to be traversed */
	public Node[] planetsLeft(RescuePhase state) {
		Set<Node> planets = state.nodes();
		Node[] all = planets.toArray(new Node[planets.size()]);
		ArrayList<Node> untouched = new ArrayList<Node>();
		
		for (Node n : all) {
			if (!collected.contains(n)) untouched.add(n);
		}
		Node[] top = untouched.toArray(new Node[untouched.size() / 10]);
		return sortGems(top);
	}
	
	/** Returns the most optimal path to take from this state to some
	 * Node without running out of fuel. This is determined by 
	 * calculating the path which has the largest ratio of gems on the 
	 * path to distance needed to traverse the path */
	public List<Node> optimalPath(RescuePhase state) {
		Node start = state.currentNode();
		Node[] most = planetsLeft(state);
		
		List<Node> path = Paths.minPath(start, most[0]);
		List<Node> bestPath = Paths.minPath(start, most[0]);
		List<Node> toHome = Paths.minPath(most[0], state.earth());
		
		double distance = Paths.pathWeight(path) + Paths.pathWeight(toHome);
		double best = Paths.pathGems(path) / Paths.pathWeight(path);
		double fuel = state.fuelRemaining();
		
		if (fuel < distance) {
			best = 0;
			bestPath = null;
		}
		
		for (Node n : most) {
			if (n != state.earth()) {
				path = Paths.minPath(start, n);
				toHome = Paths.minPath(n, state.earth());
				distance = Paths.pathWeight(path) + Paths.pathWeight(toHome);
				double ratio = Paths.pathGems(path) / distance;
				
				if ((Paths.pathGems(path) / distance) > best && (fuel >= distance)) {
					best = ratio;	
					bestPath = path;
				}
			}
		}
		if (bestPath != null) bestPath.remove(0);
		return bestPath;
	}
}