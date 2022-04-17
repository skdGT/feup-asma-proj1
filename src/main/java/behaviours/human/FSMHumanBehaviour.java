package behaviours.human;

import graph.GraphUtils;
import graph.edge.Edge;
import graph.vertex.Point;
import jade.core.Agent;
import jade.core.behaviours.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import utils.ServiceUtils;

public class FSMHumanBehaviour extends FSMBehaviour {
    static String STATE_EVAL = "EVAL";
    static String STATE_CAR = "CAR";
    static String STATE_TRC = "TRAVEL_CAR";
    static String STATE_TRD = "TRAVEL_DEFAULT";
    static String STATE_DST = "DST";
    static String STATE_CNI = "INITIATOR";
    static String STATE_CNR = "RESPONDER";

    static int EVENT_DEF = 0;
    static int EVENT_CAR = 1;
    static int EVENT_DST = 2;
    static int EVENT_INITIATE = 3;
    static int EVENT_RESPOND = 4;

    public final static String CAR_SHARE_INIT_SERVICE = "car-share-initiators";
    public final static String CAR_SHARE_RESP_SERVICE = "car-share-responders";

    protected int currentLocationIndex = 0;
    protected Graph<Point, DefaultWeightedEdge> graph;
    protected GraphPath<Point, DefaultWeightedEdge> path;
    protected boolean initiator;

    public FSMHumanBehaviour(Agent a, Graph<Point, DefaultWeightedEdge> graph, String src, String dst, boolean initiator) {
        super(a);
        this.graph = graph;
        this.path = GraphUtils.getPathFromAtoB(graph, src, dst);
        this.initiator = initiator;

        if (initiator) {
            ServiceUtils.register(myAgent, CAR_SHARE_INIT_SERVICE);
        } else {
            ServiceUtils.register(myAgent, CAR_SHARE_RESP_SERVICE);
        }

        System.out.printf("%s: Path: %s (Cost: %.02f)\n", myAgent.getLocalName(), path.getVertexList(), path.getWeight());

        this.registerFirstState(new EvaluatePathBehaviour(this), STATE_EVAL);
        this.registerLastState(new DestinationBehaviour(this), STATE_DST);

        this.registerState(new TravelDefaultBehaviour(this), STATE_TRD);
        this.registerState(new StartCarShareBehaviour(this), STATE_CAR);
        this.registerState(new TravelCarBehaviour(this), STATE_TRC);
        this.registerState(new CNIHelperBehaviour(this, myAgent), STATE_CNI);
        this.registerState(new CNRHelperBehaviour(this, myAgent), STATE_CNR);

        this.registerTransition(STATE_EVAL, STATE_CAR, EVENT_CAR);
        this.registerTransition(STATE_EVAL, STATE_DST, EVENT_DST);
        this.registerTransition(STATE_EVAL, STATE_TRD, EVENT_DEF);
        this.registerDefaultTransition(STATE_TRD, STATE_EVAL);

        this.registerTransition(STATE_CAR, STATE_CNI, EVENT_INITIATE);
        this.registerTransition(STATE_CAR, STATE_CNR, EVENT_RESPOND);
        this.registerDefaultTransition(STATE_CNI, STATE_TRC);
        this.registerDefaultTransition(STATE_CNR, STATE_TRC);

        this.registerTransition(STATE_TRC, STATE_TRC, EVENT_CAR);
        this.registerTransition(STATE_TRC, STATE_EVAL, EVENT_DEF);
        this.registerTransition(STATE_TRC, STATE_DST, EVENT_DST);
    }

    public String informTravel() {
        Point pt1 = this.path.getVertexList().get(this.currentLocationIndex);
        Point pt2 = this.path.getVertexList().get(this.currentLocationIndex + 1);
        Edge edge = (Edge) this.path.getEdgeList().get(this.currentLocationIndex);

        return String.format("%s: Moving from [%s] to [%s] by %s", this.myAgent.getLocalName(), pt1, pt2, edge);
    }
}
