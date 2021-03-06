package graph;

import agents.HumanAgent;
import graph.edge.Edge;
import graph.edge.RoadEdge;
import graph.edge.StreetEdge;
import graph.edge.SubwayEdge;
import graph.exceptions.NoRoadsException;
import graph.vertex.Point;
import graph.vertex.Semaphore;
import graph.vertex.Station;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.dot.DOTImporter;

import java.io.*;
import java.util.*;

public class GraphUtils {

    private static final String DEFAULT_GRAPH_FILENAME = "citygraph.dot";

    private GraphUtils() {
    }

    /**
     * Export a graph to DOT format with some attributes
     *
     * @param outputStream output for the DOT graph
     * @param g            original graph
     */
    public static void exportToDOT(OutputStream outputStream, Graph<Point, DefaultWeightedEdge> g) {
        DOTExporter<Point, DefaultWeightedEdge> export = new DOTExporter<>();
        export.setVertexIdProvider(Point::toString);
        export.setVertexAttributeProvider((vertex) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("type", DefaultAttribute.createAttribute(vertex.getClass().getSimpleName().toLowerCase(Locale.ROOT)));
            map.put("color", DefaultAttribute.createAttribute(((Colorable) vertex).getColor()));
            return map;
        });

        export.setEdgeAttributeProvider((edge) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("weight", DefaultAttribute.createAttribute(g.getEdgeWeight(edge)));
            map.put("label", DefaultAttribute.createAttribute(edge.toString()));
            map.put("type", DefaultAttribute.createAttribute(((Edge) edge).getType()));
            map.put("color", DefaultAttribute.createAttribute(((Colorable) edge).getColor()));
            return map;
        });
        export.exportGraph(g, outputStream);
    }

    /**
     * Import a graph from a DOT format
     *
     * @param inputStream input for the graph object
     * @return parsed graph
     */
    public static Graph<Point, DefaultWeightedEdge> getFromDOT(InputStream inputStream) {
        DOTImporter<Point, DefaultWeightedEdge> importer = new DOTImporter<>();
        Graph<Point, DefaultWeightedEdge> g = new WeightedMultigraph<>(DefaultWeightedEdge.class);

        importer.setVertexWithAttributesFactory((vertex, map) -> {
            Point point = new Point("s");

            if (map.containsKey("type")) {
                switch (map.get("type").getValue()) {
                    case "point" -> point = new Point(vertex);
                    case "station" -> point = new Station(vertex);
                    case "semaphore" -> point = new Semaphore(vertex);
                    default -> {
                    }
                }
            }

            return point;
        });

        importer.setEdgeWithAttributesFactory((map) -> {
            DefaultWeightedEdge edge = new RoadEdge();

            if (map.containsKey("type")) {
                switch (map.get("type").getValue()) {
                    case "road" -> edge = new RoadEdge();
                    case "subway" -> edge = new SubwayEdge();
                    case "street" -> edge = new StreetEdge();
                    default -> {
                    }
                }
            }

            if (map.containsKey("weight"))
                g.setEdgeWeight(edge, Double.parseDouble(map.get("weight").getValue().replace(",", ".")));

            return edge;
        });
        importer.importGraph(g, inputStream);

        return g;
    }

    /**
     * Get the shortest path from A to B
     *
     * @param graph original graph
     * @param a     source point
     * @param b     destination point
     * @return the shortest path from A to B
     */
    public static GraphPath<Point, DefaultWeightedEdge> getPathFromAtoB(Graph<Point, DefaultWeightedEdge> graph, String a, String b) {
        DijkstraShortestPath<Point, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        ShortestPathAlgorithm.SingleSourcePaths<Point, DefaultWeightedEdge> iPaths = dijkstraAlg.getPaths(Point.instance(a));

        return iPaths.getPath(Point.instance(b));
    }

    public static GraphPath<Point, DefaultWeightedEdge> getRoadPathFromAtoB(Graph<Point, DefaultWeightedEdge> graph, String a, String b) {
        // create a new graph with only road edges
        Graph<Point, DefaultWeightedEdge> roadGraph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
        graph.edgeSet().stream().filter(edge -> edge instanceof RoadEdge).forEach(edge -> {
            roadGraph.addVertex(graph.getEdgeSource(edge));
            roadGraph.addVertex(graph.getEdgeTarget(edge));
            roadGraph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge), edge);
        });

        DijkstraShortestPath<Point, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(roadGraph);
        ShortestPathAlgorithm.SingleSourcePaths<Point, DefaultWeightedEdge> iPaths = dijkstraAlg.getPaths(Point.instance(a));

        return iPaths.getPath(Point.instance(b));
    }

    /**
     * This method allows to import a graph with custom weight settings, for every weight property, if you assign
     * a non-positive value (less than or equal to zero) the weight is unchanged. You can force the shortest path
     * to not follow a certain type of edge you can assign Integer.MAX_VALUE.
     * <p>
     * We shouldn't totally remove edges since it may lead to a non-connected graph. The graph is connected on
     * street edges tho.
     *
     * @param filename     filename for the graph.dot
     * @param streetWeight weight for street edges (walking)
     * @param roadWeight   weight for road edges (car/uber)
     * @param subwayWeight weight for subway edges
     * @return a graph with custom weights
     */
    public static Graph<Point, DefaultWeightedEdge> importGraph(String filename, double streetWeight, double roadWeight, double subwayWeight) throws FileNotFoundException {
        Graph<Point, DefaultWeightedEdge> graph = GraphUtils.getFromDOT(new FileInputStream(new File(filename)));
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            if (edge instanceof StreetEdge && streetWeight > 0)
                graph.setEdgeWeight(edge, streetWeight);
            if (edge instanceof RoadEdge && roadWeight > 0)
                graph.setEdgeWeight(edge, roadWeight);
            if (edge instanceof SubwayEdge && subwayWeight > 0)
                graph.setEdgeWeight(edge, subwayWeight);
        }
        return graph;
    }

    /**
     * Import graph from a file
     *
     * @param filename file name containing a DOT format graph with required attributes
     * @return parsed graph
     * @throws FileNotFoundException when the file could not be found
     */
    public static Graph<Point, DefaultWeightedEdge> importGraph(String filename) throws FileNotFoundException {
        return GraphUtils.getFromDOT(new FileInputStream(filename));
    }

    public static Graph<Point, DefaultWeightedEdge> importDefaultGraph() throws FileNotFoundException {
        return GraphUtils.importGraph(GraphUtils.DEFAULT_GRAPH_FILENAME);
    }

    /**
     * @param graph original graph
     * @param path  related path
     * @return string containing the pretty-printed path
     * @deprecated should not be using this method, have detected some memory leaks
     * <p>
     * Method to get a Path in a pretty-printed format
     */
    @Deprecated
    public static String printPath(Graph<Point, DefaultWeightedEdge> graph, GraphPath<Point, DefaultWeightedEdge> path) {
        StringBuilder result = new StringBuilder();
        for (DefaultWeightedEdge edge : path.getEdgeList()) {
            result.append(String.format("%s --[%s]--> ", graph.getEdgeSource(edge), edge));
        }
        result.append(graph.getEdgeTarget(path.getEdgeList().get(path.getEdgeList().size() - 1)).toString());

        result.append(" -- COST: ").append(path.getWeight());

        return result.toString();
    }

    /**
     * Method to calculate the final stop for a road segment, given the current location.
     * <p>
     * Example:
     * <p>
     * Path: A -R-> B -S-> C -R-> D -R-> E -R-> F -S-> G
     * <p>
     * Legend: R-Road S-Street <p>
     * - for currentIndex = 1 -> throws <p>
     * - for currentIndex = 2 -> F <p>
     * - for currentIndex = 4 -> F <p>
     *
     * @param graph        original graph
     * @param graphPath    path
     * @param currentIndex current position - index for the point on the path
     * @return the last point for the road segment
     * @throws NoRoadsException when there's no road right ahead for the current position
     */
    public static Point roadStop(Graph<Point, DefaultWeightedEdge> graph, GraphPath<Point, DefaultWeightedEdge> graphPath, int currentIndex) throws NoRoadsException {
        if (currentIndex == graphPath.getLength()) {
            throw new NoRoadsException();
        }

        Point stop = null;
        for (int i = currentIndex; i < graphPath.getEdgeList().size(); i++) {
            DefaultWeightedEdge edge = graphPath.getEdgeList().get(i);
            if (!(edge instanceof RoadEdge)) {
                break;
            }
            stop = graphPath.getVertexList().get(i + 1);
        }

        if (stop == null) throw new NoRoadsException();

        return stop;
    }

    /**
     * Calculate the actual cost for the path
     *
     * @param graph original graph
     * @param path  resulting path
     * @return path cost
     */
    public static double calculateCost(Graph<Point, DefaultWeightedEdge> graph, GraphPath<Point, DefaultWeightedEdge> path) {
        double cost = 0.0;
        for (DefaultWeightedEdge e : path.getEdgeList()) {
            cost += graph.getEdgeWeight(e);
        }
        return cost;
    }

    public static Set<Semaphore> getSemaphores(Graph<Point, DefaultWeightedEdge> graph) {
        Set<Semaphore> semaphores = new HashSet<>();
        for (Point point : graph.vertexSet()) {
            if (point instanceof Semaphore) {
                semaphores.add((Semaphore) point);
            }
        }
        return semaphores;
    }

    /**
     * Verifies if two points are adjacent in a graph.
     *
     * @param graph the graph to be checked
     * @param a     the first point
     * @param b     the second point
     * @return true if the points are adjacent, false otherwise
     */
    public static boolean isAdjacent(Graph<Point, DefaultWeightedEdge> graph, Point a, Point b) {
        return graph.containsEdge(a, b);
    }

    public static double calculateCostForHuman(Graph<Point, DefaultWeightedEdge> graph, GraphPath<Point, DefaultWeightedEdge> path, HumanAgent agent) {
        double cost = 0.0;
        for (DefaultWeightedEdge e : path.getEdgeList()) {
            if (e instanceof RoadEdge) {
                cost += graph.getEdgeWeight(e) + graph.getEdgeWeight(e) * agent.getEnvironmentPreferences().carServiceFare();
            } else {
                cost += graph.getEdgeWeight(e);
            }
        }
        return cost;
    }
}
