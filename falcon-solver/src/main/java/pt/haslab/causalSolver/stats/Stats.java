package pt.haslab.causalSolver.stats;

/**
 * Created by nunomachado on 11/05/17.
 */
public class Stats
{

    public static long numEventsTrace = 0;

    public static long numHBConstraints = 0;

    public static double buildingModeltime = 0;

    public static double solvingTime = 0;

    public static int numVarConstraints = 0;

    public static void printStats()
    {
        System.out.println( "\n======= RESULTS =======" );
        System.out.println( "> Number of events in the trace:\t" + numEventsTrace );
        System.out.println( "> Number of constraints in the model:\t" + ( numHBConstraints + numVarConstraints ) );
        System.out.println( "   >> Variable declaration:\t\t" + numVarConstraints );
        System.out.println( "   >> Happens-before relationships:\t" + numHBConstraints );
        System.out.println(
                        "> Time to generate the constraint model:\t" + ( buildingModeltime / (double) 1000 )
                                        + " seconds" );
        System.out.println( "> Time to solve the constraints:\t" + ( solvingTime / (double) 1000 ) + " seconds" );
    }
}
