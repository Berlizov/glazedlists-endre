/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.test;

// for being a JUnit test case
import junit.framework.*;
// the core Glazed Lists package
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.util.*;
// the Glazed Lists' change objects
import ca.odell.glazedlists.event.*;
// Java collections are used for underlying data storage
import java.util.*;

/**
 * A ReadOnlyListTest tests the functionality of the ReadOnlyList
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class ReadOnlyListTest extends TestCase {

    /** attempt to modify this list */
    private List readOnly = null;
    
    /**
     * Prepare for the test.
     */
    public void setUp() {
        // create a list of data
        BasicEventList readOnlyData = new BasicEventList();
        readOnlyData.add("A");
        readOnlyData.add("B");
        readOnlyData.add("C");
        
        // our list is that data, but read only
        readOnly = new ReadOnlyList(readOnlyData);
    }

    /**
     * Clean up after the test.
     */
    public void tearDown() {
        readOnly = null;
    }

    /**
     * Verifies that the sublist is also read only.
     */
    public void testSubList() {
        try {
            readOnly.subList(0, 3).clear();
            fail();
        } catch(IllegalStateException e) {
            // read failed as expected
        }
    }

    /**
     * Verifies that the sublist is also read only.
     */
    public void testIterator() {
        try {
            Iterator i = readOnly.iterator();
            i.next();
            i.remove();
            fail();
        } catch(IllegalStateException e) {
            // read failed as expected
        }
    }
}