# Falcon Solver

Falcon Solver uses symbolic constraint solving to order a set of distributed events according to their *happens-before* relationship [1]. There exists a happens-before relationship between two events **A** and **B** if **A** causally precedes **B**. Any two events **A'** and **B'** that are not ordered by a happens-before dependency are said to be *concurrent*. 

In more detail, Falcon Solver:
1. Receives an input file with a set of events in JSON format;
2. Builds an SMT constraint model that contains symbolic variables representing the events' logical clocks and constraints encoding the happens-before relationships between them;
3. Solves the constraint model to obtain the logical clocks for the events that satisfy the causal dependencies.

**References:**

[1] L. Lamport, “Time clocks, and the ordering of events in a distributed system”, Commun. ACM, vol. 21, pp. 558–565, July 1978.

## Requirements
- Install [Z3 Theorem Prover](https://github.com/Z3Prover/z3) (make sure that the `z3` binary is in the environment PATH `/usr/bin` or `/usr/local/bin`)
- Install *falcon-taz*: `cd ../falcon-taz ; mvn package install`

## Installation

- Compile *falcon-solver*: 
```bash
mvn package
```

## Usage

(Assuming `$HOME=<path/to/falcon-solver>`)
- Run: `java -jar $HOME/target/falcon-solver-1.0-SNAPSHOT-jar-with-dependencies.jar [options]`

The options available are as:

* `--event-file <path-to-event-file>` indicates the path to the event trace in JSON format.
* `--use-timestamp <true/false>` is a boolean flag indicating whether Falcon should solve the constraints attempting to follow the original event timestamps. If false, Falcon will solve the model attempting to minimize the logical clocks. 


* `--solver-bin <path-to-solver-bin>` indicates the path to the Z3 solver binary. Default: `$HOME/lib/z3`.
* `--output-file <path-to-output-file>` indicates the path to the output file produced by Falcon Solver. The output file contains the set of events in JSON format ordered by their causal order.

Alternatively, these parameters can be configured by editing the file `$HOME/src/main/resources/causalSolver.properties` prior to building the jar.


