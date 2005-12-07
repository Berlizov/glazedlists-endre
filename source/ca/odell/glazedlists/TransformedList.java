/* Glazed Lists                                                 (c) 2003-2005 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

// the Glazed Lists' change objects
import ca.odell.glazedlists.event.*;
// for access to iterators and the Collection interface
import java.util.*;

/**
 * A convenience class for {@link EventList}s that decorate another {@link EventList}.
 * Extending classes transform their source {@link EventList} by modifying the
 * order, visibility and value of its elements.
 *
 * <p>Extending classes may implement the method {@link #getSourceIndex(int)} to
 * translate between indices of this and indices of the source.
 *
 * <p>Extending classes may implement the method {@link #isWritable()} to make the
 * source writable via this API.
 *
 * <p>Extending classes must explicitly call {@link #addListEventListener(ListEventListener)}
 * to receive change notifications from the source {@link EventList}.
 *
 * <p><strong><font color="#FF0000">Warning:</font></strong> This class is
 * thread ready but not thread safe. See {@link EventList} for an example
 * of thread safe code.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public abstract class TransformedList<S, E> extends AbstractEventList<E> implements ListEventListener<S> {

    /** the event list to transform */
    protected EventList<S> source;

    /**
     * Creates a {@link TransformedList} to transform the specified {@link EventList}.
     *
     * @param source the {@link EventList} to transform.
     */
    protected TransformedList(EventList<S> source) {
        super(source.getPublisher());
        this.source = source;
        readWriteLock = source.getReadWriteLock();
    }

    /**
     * Gets the index in the source {@link EventList} that corresponds to the
     * specified index. More formally, returns the index such that
     * <br><code>this.get(i) == source.get(getSourceIndex(i))</code> for all
     * legal values of <code>i</code>.
     */
    protected int getSourceIndex(int mutationIndex) {
        return mutationIndex;
    }

    /**
     * Gets whether the source {@link EventList} is writable via this API.
     * 
     * <p>Extending classes must override this method in order to make themselves
     * writable.
     */
    protected boolean isWritable() {
        return false;
    }

    /** {@inheritDoc} */
    public abstract void listChanged(ListEvent<S> listChanges);

    /** {@inheritDoc} */
    public boolean add(E value) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        return source.add((S) value);
    }

    /** {@inheritDoc} */
    public void add(int index, E value) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        if(index < 0 || index > size()) throw new IndexOutOfBoundsException("Cannot add at " + index + " on list of size " + size());
        int sourceIndex = 0;
        if(index < size()) sourceIndex = getSourceIndex(index);
        else sourceIndex = source.size();
        source.add(sourceIndex, (S) value);
    }

    /** {@inheritDoc} */
    public boolean addAll(int index, Collection<? extends E> values) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        if(index < 0 || index > size()) throw new IndexOutOfBoundsException("Cannot add at " + index + " on list of size " + size());
        int sourceIndex = 0;
        if(index < size()) sourceIndex = getSourceIndex(index);
        else sourceIndex = source.size();
        return source.addAll(sourceIndex, (Collection<? extends S>) values);
    }

    /** {@inheritDoc} */
    public boolean addAll(Collection<? extends E> values) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        return source.addAll((Collection<? extends S>) values);
    }

    /** {@inheritDoc} */
    public void clear() {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        // nest changes and let the other methods compose the event
        updates.beginEvent(true);
        while(!isEmpty()) {
            remove(0);
        }
        updates.commitEvent();
    }

    /** {@inheritDoc} */
    public E get(int index) {
        if(index < 0 || index >= size()) throw new IndexOutOfBoundsException("Cannot get at " + index + " on list of size " + size());
        return (E) source.get(getSourceIndex(index));
    }

    /** {@inheritDoc} */
    public E remove(int index) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        if(index < 0 || index >= size()) throw new IndexOutOfBoundsException("Cannot remove at " + index + " on list of size " + size());
        return (E) source.remove(getSourceIndex(index));
    }

    /** {@inheritDoc} */
    public boolean remove(Object toRemove) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        int index = indexOf(toRemove);
        if(index == -1) return false;
        source.remove(getSourceIndex(index));
        return true;
    }

    /** {@inheritDoc} */
    public boolean removeAll(Collection<?> collection) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        boolean changed = false;
        // nest changes and let the other methods compose the event
        updates.beginEvent(true);
        for(Iterator i = collection.iterator(); i.hasNext(); ) {
            Object value = i.next();
            int index = -1;
            while((index = indexOf(value)) != -1) {
                remove(index);
                changed = true;
            }
        }
        updates.commitEvent();
        return changed;
    }

    /** {@inheritDoc} */
    public boolean retainAll(Collection<?> values) {
        if(!isWritable()) throw new IllegalStateException("List cannot be modified in the current state");
        // nest changes and let the other methods compose the event
        updates.beginEvent(true);
        boolean changed = false;
        for(int i = 0; i < size(); ) {
            if(!values.contains(get(i))) {
                remove(i);
                changed = true;
            } else {
                i++;
            }
        }
        updates.commitEvent();
        return changed;
    }

    /** {@inheritDoc} */
    public E set(int index, E value) {
        if(!isWritable()) throw new IllegalStateException("List " + this.getClass().getName() + " cannot be modified in the current state");
        if(index < 0 || index >= size()) throw new IndexOutOfBoundsException("Cannot set at " + index + " on list of size " + size());
        return (E) source.set(getSourceIndex(index), (S) value);
    }

    /** {@inheritDoc} */
    public int size() {
        return source.size();
    }

    /**
     * Releases the resources consumed by this {@link TransformedList} so that it
     * may eventually be garbage collected.
     *
     * <p>A {@link TransformedList} will be garbage collected without a call to
     * {@link #dispose()}, but not before its source {@link EventList} is garbage
     * collected. By calling {@link #dispose()}, you allow the {@link TransformedList}
     * to be garbage collected before its source {@link EventList}. This is 
     * necessary for situations where a {@link TransformedList} is short-lived but
     * its source {@link EventList} is long-lived.
     * 
     * <p><strong><font color="#FF0000">Warning:</font></strong> It is an error
     * to call any method on a {@link TransformedList} after it has been disposed.
     */
    public void dispose() {
        source.removeListEventListener(this);
    }
}