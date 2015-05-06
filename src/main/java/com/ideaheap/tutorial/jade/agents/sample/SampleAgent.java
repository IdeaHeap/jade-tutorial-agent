package com.ideaheap.tutorial.jade.agents.sample;

import com.ideaheap.tutorial.jade.agents.sample.behaviours.IncrementBaseNumber;
import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nwertzberger on 4/23/15.
 */
public class SampleAgent extends Agent {

    private static final Logger logger = LoggerFactory.getLogger(SampleAgent.class);

    @Override
    public void setup() {
        final String otherAgentName = (String) this.getArguments()[0];
        addBehaviour(new IncrementBaseNumber(this, otherAgentName));
    }

    @Override
    public void takeDown() {
    }
}
