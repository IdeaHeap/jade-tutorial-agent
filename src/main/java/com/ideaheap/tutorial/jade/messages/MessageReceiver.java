package com.ideaheap.tutorial.jade.messages;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

/**
 * Created by nwertzberger on 5/5/15.
 */
public class MessageReceiver {
    private final Agent agent;
    private final Behaviour behavior;

    private MessageReceiver(Agent agent, Behaviour behavior) {
        this.agent = agent;
        this.behavior = behavior;
    }

    public static MessageReceiver listen(Agent agent, Behaviour behavior) {
        return new MessageReceiver(agent, behavior);
    }

    public void forInteger(IntegerMessageContentReceiver contentReceiver) {
        ACLMessage message = agent.receive();
        if (message != null) {
            contentReceiver.onMessage(Integer.valueOf(message.getContent()));
        } else {
            behavior.block();
        }
    }
}
