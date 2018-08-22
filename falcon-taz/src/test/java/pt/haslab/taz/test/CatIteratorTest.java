package pt.haslab.taz.test;

import org.junit.Test;
import pt.haslab.taz.events.CatIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class CatIteratorTest
{
    @Test
    public void testCatIterator1() {
        List<Integer> l1 = Arrays.asList(1,3,5);
        List<Integer> l2 = Arrays.asList(2,4,6);
        CatIterator<Integer> it = new CatIterator<Integer>(Arrays.asList(l1,l2));

        StringBuilder sb = new StringBuilder();

        while ( it.hasNext() ) {
            sb.append( it.next() + " " );
        }

        assertTrue( "Concat (1,3,5) with (2,4,6), expecting (1,2,3,4,5,6)",
                sb.toString().equals("1 2 3 4 5 6 "));

    }

    @Test
    public void testCatIterator2() {
        List<Integer> l1 = new ArrayList( Arrays.asList(1,3,5) );
        List<Integer> l2 = new ArrayList( Arrays.asList(2,4,6) );

        CatIterator<Integer> it = new CatIterator<Integer>(Arrays.asList(l1,l2));

        it.next();
        it.next();
        it.remove();
        it.next();
        it.remove();
        it.next();
        it.next();
        it.next();

        it = new CatIterator<Integer>(Arrays.asList(l1,l2));
        StringBuilder sb = new StringBuilder();

        while ( it.hasNext() ) {
            sb.append( it.next() + " " );
        }

        assertEquals( "Concat (1,3,5) with (2,4,6) while removing 2 and 3, expecting (1,4,5,6)", "1 4 5 6 ", sb.toString() );

    }
}
