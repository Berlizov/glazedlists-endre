/**
 * Glazed Lists
 * http://opensource.odellengineeringltd.com/glazedlists/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package com.odellengineeringltd.glazedlists;

// the Glazed Lists' change objects
import com.odellengineeringltd.glazedlists.event.*;

/**
 * An utility class that can get a list of Strings for a given object
 * for testing whether a filter matches.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public interface Filterator {

    /**
     * Gets the specified object as a list of Strings. These Strings
     * should contain all object information so that it can be compared
     * to the filter set.
     */
    public String[] getFilterStrings(Object element);
}
