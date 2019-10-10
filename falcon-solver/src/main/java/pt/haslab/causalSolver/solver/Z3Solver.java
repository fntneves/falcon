package pt.haslab.causalSolver.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.haslab.causalSolver.CausalSolver;
import pt.haslab.causalSolver.stats.Stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

/**
 * Created by nunomachado on 03/04/17.
 */
public class Z3Solver
                implements Solver
{
    private static Logger logger = LoggerFactory.getLogger( Z3Solver.class );

    private static Z3Solver instance = null;

    private static Process z3Process;

    private static BufferedReader reader;

    private static BufferedWriter writer;

    private static FileWriter outfile;

    private Z3Solver()
    {
    }

    public static Z3Solver getInstance()
    {
        if ( instance == null )
        {
            instance = new Z3Solver();
        }
        return instance;
    }

    public void init( String solverPath )
                    throws IOException
    {
        ProcessBuilder builder = new ProcessBuilder( solverPath, "-smt2", "-in" );
        builder.redirectErrorStream( true );
        z3Process = builder.start();
        InputStream pout = z3Process.getInputStream();
        OutputStream pin = z3Process.getOutputStream();

        reader = new BufferedReader( new InputStreamReader( pout ) );
        writer = new BufferedWriter( new OutputStreamWriter( pin ) );
        outfile = new FileWriter( new File( "model.txt" ) );

        this.writeConstraint( "(set-option :produce-unsat-cores true)" );
    }

    public void flush()
                    throws IOException
    {
        outfile.flush();
        writer.flush();
    }

    public void close()
                    throws IOException
    {
        z3Process.destroy();
        outfile.close();
    }

    public void writeConstraint( String constraint )
                    throws IOException
    {
        writer.write( constraint + "\n" );
        //tracer.info(constraint);
        outfile.write( constraint + "\n" );
    }

    public void writeComment( String comment )
                    throws IOException
    {
        writer.write( "; " + comment + "\n" );
        //tracer.info("\n; "+comment);
        outfile.write( "\n; " + comment + "\n" );
    }

    public String readOutputLine()
    {
        String ret = "";
        try
        {
            ret = reader.readLine();
            logger.info("Read line from solver output: " + ret);

        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean solveModel()
    {
        String isSat = "";
        try
        {
            writeConstraint( checkSat() );
            writeConstraint( "(get-model)" );
            writeConstraint( "(get-unsat-core)" );
            outfile.flush();
            writer.flush();

            isSat = readOutputLine();
            while ( !isSat.equals( "sat" ) && !isSat.equals( "unsat" ) )
            {
                isSat = readOutputLine();
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return isSat.equals( "sat" );
    }

    public String cDistinct( String exp )
    {
        return "(distinct " + exp + ")";
    }

    public String cAnd( String exp1, String exp2 )
    {
        return "(and " + exp1 + " " + exp2 + ")";
    }

    public String cAnd( String exp1 )
    {
        return "(and " + exp1 + ")";
    }

    public String cOr( String exp1, String exp2 )
    {
        return "(or " + exp1 + " " + exp2 + ")";
    }

    public String cOr( String exp1 )
    {
        return "(or " + exp1 + ")";
    }

    public String cEq( String exp1, String exp2 )
    {
        return "(= " + exp1 + " " + exp2 + ")";
    }

    public String cNeq( String exp1, String exp2 )
    {
        return "(not (= " + exp1 + " " + exp2 + "))";
    }

    public String cGeq( String exp1, String exp2 )
    {
        return "(>= " + exp1 + " " + exp2 + ")";
    }

    public String cGt( String exp1, String exp2 )
    {
        return "(> " + exp1 + " " + exp2 + ")";
    }

    public String cLeq( String exp1, String exp2 )
    {
        return "(<= " + exp1 + " " + exp2 + ")";
    }

    public String cLt( String exp1, String exp2 )
    {
        return "(< " + exp1 + " " + exp2 + ")";
    }

    public String cLt( String exp1 )
    {
        return "(< " + exp1 + " )";
    }

    public String cDiv( String exp1, String exp2 )
    {
        return "(div " + exp1 + " " + exp2 + ")";
    }

    public String cMod( String exp1, String exp2 )
    {
        return "(mod " + exp1 + " " + exp2 + ")";
    }

    public String cPlus( String exp1, String exp2 )
    {
        return "(+ " + exp1 + " " + exp2 + ")";
    }

    public String cMinus( String exp1, String exp2 )
    {
        return "(- " + exp1 + " " + exp2 + ")";
    }

    public String cMult( String exp1, String exp2 )
    {
        return "(* " + exp1 + " " + exp2 + ")";
    }

    public String cSummation( Set<String> sum )
    {
        String res = "(+";
        for ( String s : sum )
        {
            res += ( " " + s );
        }
        res += ")";
        return res;
    }

    public String cMinimize( String constraint )
    {
        return "(minimize " + constraint + ")";
    }

    public String cMaximize( String constraint )
    {
        return "(maximize " + constraint + ")";
    }

    public String declareIntVar( String varname )
    {
        String ret = "(declare-const " + varname + " Int)";
        return ret;
    }

    public String declareIntVar( String varname, int min, int max )
    {
        String ret = ( "(declare-const " + varname + " Int)\n" );
        ret += ( "(assert (and (>= " + varname + " " + min + ") (<= " + varname + " " + max + ")))" );
        return ret;
    }

    public String declareIntVar( String varname, String min, String max )
    {
        Stats.numVarConstraints++;
        String ret = ( "(declare-const " + varname + " Int)\n" );
        ret += ( "(assert (and (>= " + varname + " " + min + ") (<= " + varname + " " + max + ")))" );
        return ret;
    }

    public String postAssert( String constraint )
    {
        Stats.numHBConstraints++;
        return ( "(assert " + constraint + ")" );
    }

    public String postNamedAssert( String constraint, String label )
    {
        Stats.numHBConstraints++;
        return ( "(assert (! " + constraint + ":named " + label + "))" );
    }

    public String postSoftAssert( String constraint )
    {
        Stats.numHBConstraints++;
        return ( "(assert-soft " + constraint + ")" );
    }

    public String postNamedSoftAssert( String constraint, String label )
    {
        Stats.numHBConstraints++;
        return ( "(assert-soft (! " + constraint + ":named " + label + "))" );
    }

    public String push()
    {
        return "(push)";
    }

    public String pop()
    {
        return "(pop)";
    }

    public String checkSat()
    {
        return "(check-sat)";
    }

}
