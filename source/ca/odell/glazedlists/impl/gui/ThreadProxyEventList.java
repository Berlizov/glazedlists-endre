/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.impl.gui;

// Java collections are used for underlying data storage
import java.util.*;
// the core Glazed Lists packages
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.event.*;

/**
 * An {@link EventList} that only forwards its events on a user-interface thread,
 * regardless of the thread of their origin.
 *
 * <p>While the UI thread is getting to the events to forward, more events may
 * arrive. This means that multiple changes may have occurred before the user interface
 * can be notified. One problem with this limitation is that some of these changes
 * may contradict one another. For example, an inserted item may be later removed 
 * before either event is processed. To overcome this limitation, this class uses
 * the change-contradiction resolving logic of {@link ListEventAssembler}.
 *
 * <p>The flow of events is as follows:
 * <ol><li>Any thread makes a change to the source list and calls
 *     {@link ThreadProxyEventList#listChanged(ListEvent)}.
 *     <li>The event is enqueued and the user interface thread is notified that
 *     it has a task in the future.
 * </ol>
 * <p>Then some time later in the future after one or more events have been enqueued,
 * the user-interface thread gets to its queue:
 * <ol><li>First it acquires a lock so that no more changes can occur
 *     <li>The complete set of changes are combined into one change. Currently this
 *     implementation does a best effort on conflict resolution. It is limited in
 *     its handling of reordering events, as caused by changing a {@link SortedList}
 *     {@link Comparator}.
 *     <li>The completed set of events is fired
 *     <li>The first listener is the {@link ThreadProxyEventList} itself. It listens
 *     to its own event because this event will be free of conflicts. It applies the
 *     changes to its own internal copy of the data.
 *     <li>All other listeners are notified of the change
 *     <li>The lock is released.
 * </ol>
 *
 * <p>The {@link ThreadProxyEventList} keeps a private copy of the source {@link EventList}'s
 * elements. This enables interested classes to read a consistent (albeit potentially
 * out of date) view of the data at all times.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public abstract class ThreadProxyEventList extends TransformedList {
    
    /** a local cache of the source list */
    private List localCache = new ArrayList();
    
    /** propagates events on the Swing thread */
    private UpdateRunner updateRunner = new UpdateRunner();
    
    /** the pending events, possibly empty, one, or multiple */
    private ListEvent listChanges = null;
        
    /**
     */
    public ThreadProxyEventList(EventList source) {
        super(source);
        
        // populate the initial cache value
        localCache.addAll(source);
        
        // handle my own events to update the internal state
        this.addListEventListener(updateRunner);
        
        // handle changes in the source event list
        source.addListEventListener(this);
    }

    /** {@inheritDoc} */
    public final void listChanged(ListEvent listChanges) {
        // ensure we have a Swing proxy for the update event
        this.listChanges = listChanges;

        // forward the event on the appropriate thread
        schedule(updateRunner);
    }
    
    /**
     * Schedule the specified runnable to be run on the proxied thread.
     */
    protected abstract void schedule(Runnable runnable);    

    /** {@inheritDoc} */
    public final int size() {
        return localCache.size();
    }
    
    /** {@inheritDoc} */
    public final Object get(int index) {
        return localCache.get(index);
    }

    /** {@inheritDoc} */
    protected final boolean isWritable() {
        return true;
    }
    
    
    /**
     * Updates the internal data over the Swing thread.
     *
     * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
     */
    private class UpdateRunner implements Runnable, ListEventListener {
    
        /**
         * When run, this combines all events thus far and forwards them.
         *
         * <p>If a reordering event is being forwarded, the reordering may be lost
         * if it arrives simultaneously with another event. This is somewhat of a
         * hack for the time being. Hopefully later we can refine this so that a
         * new event is created with these changes properly.
         */
        public void run() {
            getReadWriteLock().writeLock().lock();
            try {
                updates.beginEvent(true);
                boolean forwardedEvents = false;
                while(listChanges.hasNext()) {
                    if(listChanges.isReordering() && !forwardedEvents) {
                        updates.reorder(listChanges.getReorderMap());
                    } else {
                        while(listChanges.next()) {
                            updates.addChange(listChanges.getType(), listChanges.getIndex());
                        }
                    }
                    forwardedEvents = true;
                }
                updates.commitEvent();
            } finally {
                getReadWriteLock().writeLock().unlock();
            }
        }
    
        /**
         * Update local state as a consequence of the change event.
         */
        public void listChanged(ListEvent listChanges) {
            // handle reordering events
            if(listChanges.isReordering()) {
                List newLocalCache = new ArrayList();
                int[] sourceReorderMap = listChanges.getReorderMap();
                for(int i = 0; i < sourceReorderMap.length; i++) {
                    newLocalCache.add(i, localCache.get(sourceReorderMap[i]));
                }
                localCache.clear();
                localCache.addAll(newLocalCache);
    
            // handle everything else
            } else {
                while(listChanges.next()) {
                    int sourceIndex = listChanges.getIndex();
                    int changeType = listChanges.getType();
    
                    if(changeType == ListEvent.DELETE) {
                        localCache.remove(sourceIndex);
                    } else if(changeType == ListEvent.INSERT) {
                        localCache.add(sourceIndex, source.get(sourceIndex));
                    } else if(changeType == ListEvent.UPDATE) {
                        localCache.set(sourceIndex, source.get(sourceIndex));
                    }
                }
            }
        }
    }
}
