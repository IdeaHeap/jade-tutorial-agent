#Introduction

This is the markdown version of the tutorial [On IdeaHeap.com](http://www.ideaheap.com/2015/05/jade-setup-for-beginners/).

##Prerequisites

This howto is written assuming that you have basic understanding of Java and that you are capable of downloading [Maven](https://maven.apache.org/) and get it on your command line. You may also want to choose an IDE (Eclipse or Intellij are both good choices).

For more information about what JADE is, visit [Their main website](http://jade.tilab.com/). It's a messaging framework and a collection of classes that allow the rapid creation of agent-based applications.

##What's Agent-based programming?

Agent based programming is a paradigm where modules of state and code work together to create an independent module that can "observe" the world around it and "output" actions into this world. A great example of a simple agent is an event-based service, where it is listening on a port and, once it has received input, reacts to this.

The model feels very natural to a person familiar with object oriented programming. JADE does a good job of enforcing this paradigm and, if you write a well-designed JADE application, your code will be relatively performant, as JADE is non-blocking, and uses only one thread per agent.

#Environment Setup

##Maven configuration

Maven is a build system that takes care of bringing in dependencies, and allows you to write how to build your application in a declarative manner. It has a learning curve, but for the sake of this tutorial, you can use the code below with the commands given, and you'll probably only have to worry about adding dependencies as you find things.

Your new project can start with the **pom.xml** in the example source. In this POM, we have included a logging framework (I don't want to bind everything to JADE), and the tools needed to include JADE.  The build profiles specify two different ways to start jade using two configuration files, which will be explained in the next section.  

##Maven Calls

Once this is set up, your application will be able to start using the following two commands:

    mvn -Pjade-main exec:java
    mvn -Pjade-agent exec:java

The first command starts our profile with a configuration that creates a default main container with no custom agents. The second command starts our profile, which starts a headless container running any custom agents specified in that configuration file.

#Container Configurations

The two configuration files mentioned in the exec plugin configurations define how to start the jade container.

##Jade Main Container Configuration

JADE requires a main container to be running in order to be up, but it also supports the creation of additional containers to run agents in. A container can run many agents, and containers can be on many computers, allowing a distributed architecture. There can be only one main JADE container in a platform, but a backup container can be running that will take over in the event the original main container fails.

The first configuration file is located in *src/main/resource/jade-main-container.properties*. It contains the following:

    gui=true
    host=localhost
    port=10099
    local-port=10099
    jade_domain_df_autocleanup=true

This will set up a main container that has a gui available for debugging and controlling your jade installation. It's configured to run on port 10099 with this.

The autocleanup line is a setting for jade's df agent, which is reponsible for knowing where different agents are that are capable of communicating about different message types. It doesn't clean up listing for killed agents by default, but with this configuration, it does.

##Jade Agent Container Configuration

The agent container is designed to be unobtrusive and easy to kill. It is headless, and connects to the main container. Below is our agent container configuration.

    agents=\
        sample-agent-1:com.ideaheap.tutorial.jade.agents.sample.SampleAgent(sample-agent-2);\
        sample-agent-2:com.ideaheap.tutorial.jade.agents.sample.SampleAgent(sample-agent-1)
    port=10099
    host=localhost
    main=false
    no-display=true

Because Maven has a little trouble cleaning up un-stopped projects, if you want to create a separate project only for starting jade-main, that is fine. Having this run as a separate executable lets you keep its gui active while you are starting and killing the other jvm started with the *jade-agent* profile.

#Creating a sample agent

This tutorial doesn't just give an example agent, it also is a recommendation on how to go about architecting your agent-based program. Agents naturally create modular code but clearly defining how modules should communicate - through messages between agents. I have opted for the following directory structure:

<pre>
base.package.name.agents.agentName.AgentNameAgent
                                  .behaviors.Behavior
</pre>

Each agent follows this packaging setup. We'll now go through each of these things.

##The Agent Class

In the directory structure above, this class would be **base.package.name.agents.agentName.AgentNameAgent**.  I come from a background of very long names, and memory is cheap these days, so this does not bother me. This package scheme creates a natural encapsulation for all the helper classes that show up as you build out an agent.

The AgentNameAgent is responsible for defining an agent configuration, and the code written inside an agent follows the design of "Inversion of Control" or "Dependency Injection" without the use of a framework (like Spring).

In the example project, this was named the "SampleAgent". Below is the code for this agent:

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

This is all that should be in an agent class, save registry code (which will be covered in the next tutorial). The Agent is responsible for wiring together the classes that make up its behaviors.

##The Behaviour Class

The behaviour class is where all the action is. There are some very important notes to this class: the most important of which is that everything done in this class must be non-blocking (i.e. don't go making long-running calls to databases or external services on the main thread here). Each agent has one thread that multiplexes EVERY behaviour.

This sort of requirement basically guarantees that you'll be writing a state machine, because complicated interactions will naturally lead to this. The sample behavior is a three part behavior that includes sending, receiving, and replying to a message.

###Method Breakdown

#####action()
This class is run by a small state machine defined here:

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

This method, which is a required part of any class implementing Behaviour, is called over and over again. You may be wondering what that *block()* method is doing there after I had emphasized how important it is for things to be non-blocking. In this case, block() is actually a signal to the containing agent that it should not call this action again until after it has a reason to think things have changed. This is most often the receipt of a message.  In this way, *block()* is actually non-blocking! This also means that any code put after a block() is still going to run immediately.

#####startIncrementing()

Our behavior begins with a state that creates and sends a message off to a designated receiving agent. The message is built to send just the number 1 to the agent we sent in as a part of our constructor. The builder for this will also be discussed. 

    private void startIncrementing() {
        agent.send(inform().toLocal(otherAgentName).withContent(1).build());
        state = State.CONTINUE_INCREMENTING;
    }

#####continueIncrementing()

The continueIncrementing state utilizes a wrapper that checks for a pending message, and if it exists, parses the content of that message into an integer and calls the defined callback:

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

The callback is set to send the integer it was given as a message to the agent defined in its constructor.

#####stopIncrementing()

The stopIncrementing class showcases options available to you if you are sure that your experiment is done. You can just kill the agent that was doing the work, you can just set your behavior's done() method to return "true", or you can do what this example shows: kill the entire container.

    private void stopIncrementing() {
        listen(agent, this).forInteger((toIgnore) -> {
            logger.info("I'm just going to ignore this: " + toIgnore);
            ContainerKiller.killContainerOf(agent);
        });
    }

#Conclusion

With this basic setup, you should be able to create a simple group of agents that can communicate with each other inside of a maven managed project. The **listen** and **inform** functions you have seen are part of a small set of classes in this project which encourage a more fluent-style use of JADE. They are just the beginning, and will be built out considerably.

**All code is available for free on [GitHub](https://github.com/IdeaHeap/jade-tutorial-agent).**

**Challenge:** this example only has two agents communicating with each other for incrementing. Try making a ring of three. What happens if it's an agent "love triangle", and two agents are set to send messages to the same agent?


