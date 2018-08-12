# Falcon: Practical Log-based Analysis for Distributed Systems

Falcon is a tool aimed at easing the understanding of distributed system executions. Falcon ingests events logged by popular tracing tools (e.g. eBPF, strace, log4j) and merges them into a single causally-coherent execution trace. To further improve the reasoning about the runtime behavior of the system, Falcon also generates a space-time diagram depicting the events and their happens-before dependencies.


The architecture of Falcon is composed of three main components that operate together as a pipeline:

- **falcon-tracer** uses eBPF to trace events of interest (e.g. start, end, fork, join, send, receive, etc) at runtime. 
- **falcon-solver** combines the events into a global execution trace that preserves causality. This is achieved by *i)* building a symbolic constraint model that encodes the *happens-before* relationships between events, *ii)* using an SMT solver to solve the constraints and assign a logical clock to each event such that all causal dependencies are satisfied.
- **falcon-visualizer** draws a space-time diagram that enables a visual analysis of the whole execution.

For additional details about Falcon, please check our [DSN'18 paper](https://ieeexplore.ieee.org/abstract/document/8416513/).


## Installation

Falcon's components are independent and, therefore, can be leveraged by different projects. To set up the full pipeline, install each particular component according to the following instructions:
- [Install falcon-tracer](https://github.com/fntneves/falcon/tree/master/falcon-tracer)
- [Install falcon-solver](https://github.com/fntneves/falcon/tree/master/falcon-solver)
- [Install falcon-visualizer](https://github.com/fntneves/falcon/tree/master/falcon-visualizer)

## Usage

Falcon operates in three different phases, namely *event tracing*, *causality inference*, and *space-time diagram visualization*. Each phase is performed by a particular component of Falcon: 
- [Event tracing with falcon-tracer](https://github.com/fntneves/falcon/tree/master/falcon-tracer)
- [Causality inference with falcon-solver](https://github.com/fntneves/falcon/tree/master/falcon-solver)
- [Diagram visualization with falcon-visualizer](https://github.com/fntneves/falcon/tree/master/falcon-visualizer)

For a concrete example of how to use falcon in practice, please check our [Tutorial - Falcon with Zookeeper](https://github.com/fntneves/falcon/tree/master/docs/examples/zookeeper).


