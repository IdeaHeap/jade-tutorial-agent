package com.ideaheap.tutorial.jade.agents.sample.behaviours;

import com.ideaheap.tutorial.jade.tools.ContainerKiller;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ideaheap.tutorial.jade.messages.MessageBuilder.inform;
import static com.ideaheap.tutorial.jade.messages.MessageReceiver.listen;

/**
 * Created by nwertzberger on 4/23/15.
 */
public class IncrementBaseNumber extends Behaviour {
    private static final Logger logger = LoggerFactory.getLogger(IncrementBaseNumber.class);
    private static final int MAX_INCREMENT = 10;

    private enum State {
        START_INCREMENTING, CONTINUE_INCREMENTING, STOP_INCREMENTING
    }

    private final Agent agent;
    private final String otherAgentName;
    private State state;

    public IncrementBaseNumber(Agent agent, String otherAgentName) {
        this.agent = agent;
        this.otherAgentName = otherAgentName;
        this.state = State.START_INCREMENTING;
    }

    @Override
    public void action() {
        switch (state) {
            case START_INCREMENTING:
                startIncrementing();
                break;
            case CONTINUE_INCREMENTING:
                continueIncrementing();
                break;
            case STOP_INCREMENTING:
                stopIncrementing();
                break;
            default:
                block();
        }
    }

    private void startIncrementing() {
        agent.send(inform().toLocal(otherAgentName).withContent(1).build());
        state = State.CONTINUE_INCREMENTING;
    }

    private void continueIncrementing() {
        listen(agent, this).forInteger((toIncrement) -> {
            logger.info("Recieved " + toIncrement);
            toIncrement++;
            agent.send(inform().toLocal(otherAgentName).withContent(toIncrement).build());
            if (toIncrement > MAX_INCREMENT) {
                state = State.STOP_INCREMENTING;
            }
        });
    }

    private void stopIncrementing() {
        listen(agent, this).forInteger((toIgnore) -> {
            logger.info("I'm just going to ignore this: " + toIgnore);
            ContainerKiller.killContainerOf(agent);
        });
    }

    @Override
    public boolean done() {
        return false;
    }
}
