package agents;

import behaviours.DisplayMessagesBehaviour;
import behaviours.LaunchMessageBehaviour;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;

/**
 * Rests in a service and waits for new messages. Then prints them.
 */
public class BillboardAgent extends Agent {

    private DisplayMessagesBehaviour displayMessagesBehaviour;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        String serviceName = (String) args[0];
        this.displayMessagesBehaviour = new DisplayMessagesBehaviour(this, serviceName);

        this.addBehaviour(this.displayMessagesBehaviour);
    }

    @Override
    protected void takeDown() {
        this.displayMessagesBehaviour.stop();
    }
}