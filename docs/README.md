# Tutorial - Falcon with Zookeeper

This tutorial shows how one can use Falcon to analyze an execution of [Apache Zookeeper](https://zookeeper.apache.org). The tutorial is divided into the three steps, corresponding to the three phases of the Falcon's workflow pipeline.

TO-DO: describe the setup of the experiment

**Requirements:**

- [Apache Zookeeper](https://zookeeper.apache.org/doc/current/zookeeperStarted.html)

## Initial Setup

## Event Tracing

TO-DO: describe how to run falcon-tracer

## Causality Inference

After running Zookeeper and collecting the components' traces, we need to combine them into a single execution event trace. 

```bash
cat zktrace/*.log > zktrace_full.log 
```

Note that, at this point, the full event trace is simply a concatenation of each component's log without any causal order established across the events. To obtain a coherent execution trace, we rely on *falcon-solver* to infer the happens-before relationships.

```bash
java -jar falcon-solver/target/falcon-solver-1.0-SNAPSHOT-jar-with-dependencies.jar --event-file zktrace_full.log --output-file zktrace_full_ordered.log
```

If everything goes as expected, falcon-solver should yield the causally-ordered execution trace after some seconds. Falcon-solver also outputs some statistics about the constraint solving procedure such as the number of constraints in the model and the solving time. 

## Diagram Visualization

The last step of Falcon's pipeline consists of generating and visualizing the space-time diagram of the execution. This is done by, first, starting the falcon-visualizer:

```bash
cd falcon-visualizer
npm run dev
```

After the browser opens, click the button *Choose File* and select file `zktrace_full_ordered.log`. Then, click the button *Tick* to start drawing the diagram. By systematically clicking the button (or pressing the space bar), new logical timestamps will appear on the left side of the screen and the events occurring at those timestamps will be drawn along with their causal dependencies.

