/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package com.odellengineeringltd.glazedlists.swing;

// the core Glazed Lists packages
import com.odellengineeringltd.glazedlists.*;
import com.odellengineeringltd.glazedlists.event.*;
// Swing toolkit stuff for displaying widgets
import javax.swing.*;
import java.awt.GridBagLayout;
// for responding to user actions
import java.awt.event.*;
import java.awt.Point;
import javax.swing.event.*;
// this class uses tables for displaying message lists
import java.util.*;

/**
 * A JList that displays the contents of an event-driven list.
 *
 * <p>The EventJList class is <strong>not thread-safe</strong>. Unless otherwise
 * noted, all methods are only safe to be called from the event dispatch thread.
 * To do this programmatically, use <code>SwingUtilities.invokeAndWait()</code>.
 *
 * <p>I have implemented EventJList. The class shares the following with ListTable:
 * <li>SelectionListener interface
 * <li>SelectionList / Selection Model
 *
 * <p>This class never batches groups of changes like ListTable does. It also
 * does not use a Mutable change event. It may be necessary to create a mutable
 * ListDataEvent if change event creation proves to be a bottleneck.
 *
 * <p>This class still does not have any extra renderer support. For now if
 * styled rendering is necessary, the use of ListTable is a sufficient work
 * around.
 * 
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=14">Bug 14</a>
 *
 * @see SwingUtilities#invokeAndWait(Runnable)
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class EventListModel implements ListEventListener, ListModel {

    /** the list data */
    protected EventList source;

    /** whom to notify of data changes */
    private ArrayList listeners = new ArrayList();

    /** whenever a list change covers greater than this many rows, redraw the whole thing */
    public int changeSizeRepaintAllThreshhold = Integer.MAX_VALUE;
    
    /** recycle the list data event to prevent unnecessary object creation */
    protected MutableListDataEvent listDataEvent = new MutableListDataEvent(this);

    /**
     * Creates a new widget that renders the specified list.
     */
    public EventListModel(EventList source) {
        this.source = source;

        // prepare listeners
        source.addListEventListener(new EventThreadProxy(this));
    }
    
    /**
     * For implementing the ListEventListener interface. This sends changes
     * to the table which can repaint the table cells. Because this class uses
     * a EventThreadProxy, it is guaranteed that all natural
     * calls to this method use the Swing thread.
     *
     * <p>This always sends discrete changes for the complete size of the list.
     * It may be more efficient to implement a threshhold where a large list
     * of changes are grouped together as a single change. This is how the
     * ListTable accepts large change events.
     */
    public void listChanged(ListEvent listChanges) {
        // when all events hae already been processed by clearing the event queue
        if(!listChanges.hasNext()) return;

        // for all changes, one block at a time
        while(listChanges.nextBlock()) {
            // get the current change info
            int startIndex = listChanges.getBlockStartIndex();
            int endIndex = listChanges.getBlockEndIndex();
            int changeType = listChanges.getType();

            // create a table model event for this block
            listDataEvent.setRange(startIndex, endIndex);
            if(changeType == ListEvent.INSERT) listDataEvent.setType(ListDataEvent.INTERVAL_ADDED);
            else if(changeType == ListEvent.DELETE) listDataEvent.setType(ListDataEvent.INTERVAL_REMOVED);
            else if(changeType == ListEvent.UPDATE) listDataEvent.setType(ListDataEvent.CONTENTS_CHANGED);

            // fire an event to notify all listeners
            fireListDataEvent(listDataEvent);
        }
    }

    /**
     * Retrieves the value at the specified location from the table.
     * 
     * Before every get, we need to validate the row because there may be an
     * update waiting in the event queue. For example, it is possible that
     * the source list has been updated by a database thread. Such a change
     * may have been sent as notification, but after this request in the
     * event queue. In the case where a row is no longer available, null is
     * returned. The value returned is insignificant in this case because the
     * Event queue will very shortly be repainting (or removing) the row
     * anyway.
     *
     * @see com.odellengineeringltd.glazedlists.swing.EventTableModel#getValueAt(int,int) ListTable
     */
    public Object getElementAt(int index) {
        source.getReadWriteLock().readLock().lock();
        try {
            // ensure that this value still exists before retrieval
            if(index < source.size()) {
                return source.get(index);
            } else {
                return null;
            }
        } finally {
            source.getReadWriteLock().readLock().unlock();
        }
    }
    
    /**
     * Gets the size of the list.
     */
    public int getSize() {
        source.getReadWriteLock().readLock().lock();
        try {
            return source.size();
        } finally {
            source.getReadWriteLock().readLock().unlock();
        }
    }

    /**
     * Registers the specified ListDataListener to receive updates whenever
     * this list changes.
     *
     * <p>The specified ListDataListener must <strong>not</strong> save a
     * reference to the ListDataEvent beyond the end of the notification
     * method. This is because the ListDataEvent is re-used to increase
     * the performance of this implementation.
     */
    public void addListDataListener(ListDataListener listDataListener) {
        listeners.add(listDataListener);
    }
    /**
     * Deregisters the specified ListDataListener from receiving updates
     * whenever this list changes.
     */
    public void removeListDataListener(ListDataListener listDataListener) {
        listeners.remove(listDataListener);
    }

    /**
     * Notifies all ListDataListeners about one block of changes in the list.
     */
    protected void fireListDataEvent(ListDataEvent listDataEvent) {
        // notify all listeners about the event
        for(int i = 0; i < listeners.size(); i++) {
            ListDataListener listDataListener = (ListDataListener)listeners.get(i);
            if(listDataEvent.getType() == ListDataEvent.CONTENTS_CHANGED) {
                listDataListener.contentsChanged(listDataEvent);
            } else if(listDataEvent.getType() == ListDataEvent.INTERVAL_ADDED) {
                listDataListener.intervalAdded(listDataEvent);
            } else if(listDataEvent.getType() == ListDataEvent.INTERVAL_REMOVED) {
                listDataListener.intervalRemoved(listDataEvent);
            }
        }
    }
}
