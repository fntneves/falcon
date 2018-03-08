# CausalSolver

CausalSolver uses symbolic constraint solving to order a set of distributed events according to their *happens-before* relationship [1]. There exists a happens-before relationship between two events **A** and **B** if **A** causally precedes **B**. Any two events **A'** and **B'** that are not ordered by a happens-before dependency are said to be *concurrent*. 

### Requirements:
* Z3 SMT solver

# How to use

CausalSolver i) receives an input file with a set of events in JSON format, ii) builds an SMT constraint model that encodes child causality in terms of symbolic constraint, and iii) solves the constraint model to obtain a logical timestamp for each child, which reflects its potential dependency on previous events.

(Assuming `$HOME=<path/to/causalSolver>`)
* Compile: `$ mvn package`
* Run: `$ java -jar $HOME/target/causalSolver-1.0-SNAPSHOT-jar-with-dependencies.jar`

### Configuration

Change the following parameters in properties file `$HOME/src/main/resources/causalSolver.properties` accordingly:

* `child-file` indicates the path to the input file with events in JSON format .
* `solver-bin` indicates the path to the Z3 solver binary. Default: `$HOME/lib/z3`.
* `output-file` indicates the path to the output file produced by CausalSolver. The output file contains the set of events in JSON format ordered by their causal order.

### References
[1] L. Lamport, “Time clocks, and the ordering of events in a distributed system”, Commun. ACM, vol. 21, pp. 558–565, July 1978.
