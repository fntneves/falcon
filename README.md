# Falcon -- Practical Log-based Analysis for Distributed Systems

Falcon is a tool to extract and visualize causal dependencies between distributed events logged by commodity tracing tools (e.g. eBPF, strace, log4j). Falcon is composed of three main components that operate together as a pipeline:

- **falcon-tracer** uses eBPF to trace events of interest (e.g. start, end, fork, join, send, receive, etc) at runtime. 
- **falcon-solver** combines the events into a global execution trace that preserves causality. This is achieved by *i)* building a symbolic constraint model that encodes the *happens-before* relationships between events, *ii)* using an SMT solver to solve the constraints and assign a logical clock to each event such that all causal dependencies are satisfied.
- **falcon-visualizer** draws a space-time diagram that enables a visual analysis of the whole (coherent) execution.

For additional details about Falcon, please check our [DSN'18 paper (to appear)](https://github.com/fntneves/falcon).


## Installation

The modular architecture of Falcon allows its components to be used in separate (and leveraged by different projects). To set up the complete Falcon pipeline, install each particular component according to the following instructions:
- [Install falcon-tracer](https://github.com/fntneves/falcon/tree/master/falcon-tracer)
- [Install falcon-solver](https://github.com/fntneves/falcon/tree/master/falcon-solver)
- [Install falcon-visualizer](https://github.com/fntneves/falcon/tree/master/falcon-visualizer)

## Usage

Falcon operates in three different phases, namely *event tracing*, *causality inference*, and *space-time diagram visualization*. Each phase is performed by a particular component of Falcon: 
- [Event tracing with falcon-tracer](https://github.com/fntneves/falcon/tree/master/falcon-tracer)
- [Causality inference with falcon-solver](https://github.com/fntneves/falcon/tree/master/falcon-solver)
- [Diagram visualization with falcon-visualizer](https://github.com/fntneves/falcon/tree/master/falcon-visualizer)

For a concrete example of how to use falcon in practice, please check our [Tutorial - Falcon with Zookeeper](https://github.com/fntneves/falcon).


