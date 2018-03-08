package pt.haslab.causalSolver.stats;

/**
 * Created by nunomachado on 11/05/17.
 */
public class Stats {

    public static long numEventsTrace = 0;
    public static long numConstraints = 0;
    public static double buildingModeltime = 0;
    public static double solvingTime = 0;
    public static int numVarConstraints = 0;

    public static void printStats(){
        System.out.println("\n======= RESULTS =======");
        System.out.println("> Number of events in trace:\t\t"+numEventsTrace);
        System.out.println("> Number of variable constraints in model:\t"+numVarConstraints);
        System.out.println("> Number of constraints in model:\t"+numConstraints);
        System.out.println("> Time to generate constraint model:\t"+(buildingModeltime/(double)1000)+" seconds");
        System.out.println("> Time to solve causal order:\t"+(solvingTime/(double)1000)+" seconds");
    }
}
