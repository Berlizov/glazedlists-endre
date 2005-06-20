/* Glazed Lists                                                 (c) 2003-2005 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import java.util.*;
// for weak reference proxying
import ca.odell.glazedlists.event.ListEventListener;
// for access to volatile classes
import ca.odell.glazedlists.impl.*;
import ca.odell.glazedlists.impl.sort.*;
import ca.odell.glazedlists.impl.beans.*;
import ca.odell.glazedlists.impl.matchers.*;
// implemented interfaces
import ca.odell.glazedlists.gui.*;
import ca.odell.glazedlists.matchers.*;
import java.util.Comparator;


/**
 * A factory for creating all sorts of objects to be used with Glazed Lists.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public final class GlazedLists {

    /**
     * A dummy constructor to prevent instantiation of this class
     */
    private GlazedLists() {
        throw new UnsupportedOperationException();
    }

    // Utility Methods // // // // // // // // // // // // // // // // // // //

    /**
     * Replace the complete contents of the target {@link EventList} with the complete
     * contents of the source {@link EventList} while making as few list changes
     * as possible.
     *
     * <p>In a multi-threaded environment, it is necessary that the caller obtain
     * the write lock for the target list before this method is invoked. If the
     * source list is an {@link EventList}, its read lock must also be acquired.
     *
     * <p>This method shall be used when it is necessary to update an EventList
     * to a newer state while minimizing the number of change events fired. It
     * is desirable over {@link List#clear() clear()}; {@link List#addAll(Collection) addAll()}
     * because it will not cause selection to be lost if unnecessary. It is also
     * useful where firing changes may be expensive, such as when they will cause
     * writes to disk or the network.
     *
     * <p>This is implemented using Eugene W. Myer's paper, "An O(ND) Difference
     * Algorithm and Its Variations", the same algorithm found in GNU diff.
     *
     * @param updates whether to fire update events for Objects that are equal in
     *      both {@link List}s.
     */
    public static void replaceAll(EventList target, List source, boolean updates) {
        Diff.replaceAll(target, source, updates);
    }


    // Comparators // // // // // // // // // // // // // // // // // // // //

    /** Provide Singleton access for all Comparators with no internal state */
    private static Comparator booleanComparator = null;
    private static Comparator comparableComparator = null;
    private static Comparator reversedComparable = null;

    /**
     * Creates a {@link Comparator} that uses Reflection to compare two instances
     * of the specified {@link Class} by the given JavaBean property.  The JavaBean
     * property must implement {@link Comparable}.
     */
    public static Comparator beanPropertyComparator(Class className, String property) {
        return beanPropertyComparator(className, property, comparableComparator());
    }

    /**
     * Creates a {@link Comparator} that uses Reflection to compare two instances
     * of the specified {@link Class} by the given JavaBean property.  The JavaBean
     * property is compared using the provided {@link Comparator}.
     */
    public static Comparator beanPropertyComparator(Class className, String property, Comparator propertyComparator) {
        return new BeanPropertyComparator(className, property, propertyComparator);
    }

    /**
     * Creates a {@link Comparator} for use with {@link Boolean} objects.
     */
    public static Comparator booleanComparator() {
        if(booleanComparator == null) booleanComparator = new BooleanComparator();
        return booleanComparator;
    }

    /**
     * Creates a {@link Comparator} that compares {@link String} objects in
     * a case-insensitive way.  This {@link Comparator} is equivalent to using
     * {@link String#CASE_INSENSITIVE_ORDER} and exists here for convenience.
     */
    public static Comparator caseInsensitiveComparator() {
        return String.CASE_INSENSITIVE_ORDER;
    }

    /**
     * Creates a chain of {@link Comparator}s that applies the provided
     * {@link Comparator}s in the sequence specified until differences or
     * absoulute equality.is determined.
     */
    public static Comparator chainComparators(List comparators) {
        return new ComparatorChain(comparators);
    }

    /**
     * Creates a {@link Comparator} that compares {@link Comparable} objects.
     */
    public static Comparator comparableComparator() {
        if(comparableComparator == null) comparableComparator = new ComparableComparator();
        return comparableComparator;
    }

    /**
     * Creates a reverse {@link Comparator} that works for {@link Comparable} objects.
     */
    public static Comparator reverseComparator() {
        if(reversedComparable == null) reversedComparable = reverseComparator(comparableComparator());
        return reversedComparable;
    }

    /**
     * Creates a reverse {@link Comparator} that inverts the given {@link Comparator}.
     */
    public static Comparator reverseComparator(Comparator forward) {
        return new ReverseComparator(forward);
    }

    // TableFormats // // // // // // // // // // // // // // // // // // // //

    /**
     * Creates a {@link TableFormat} that binds JavaBean properties to
     * table columns via Reflection.
     */
    public static TableFormat tableFormat(String[] propertyNames, String[] columnLabels) {
        return new BeanTableFormat(null, propertyNames, columnLabels);
    }

    /**
     * Creates a {@link TableFormat} that binds JavaBean properties to
     * table columns via Reflection.
     *
     * @param baseClass the class of the Object to divide into columns. If specified,
     *      the returned class will provide implementation of
     *      {@link AdvancedTableFormat#getColumnClass(int)} and
     *      {@link AdvancedTableFormat#getColumnComparator(int)} by examining the
     *      classes of the column value.
     */
    public static TableFormat tableFormat(Class baseClass, String[] propertyNames, String[] columnLabels) {
        return new BeanTableFormat(baseClass, propertyNames, columnLabels);
    }

    /**
     * Creates a {@link TableFormat} that binds JavaBean properties to
     * table columns via Reflection. The returned {@link TableFormat} implements
     * {@link WritableTableFormat} and may be used for an editable table.
     */
    public static TableFormat tableFormat(String[] propertyNames, String[] columnLabels, boolean[] editable) {
        return new BeanTableFormat(null, propertyNames, columnLabels, editable);
    }

    /**
     * Creates a {@link TableFormat} that binds JavaBean properties to
     * table columns via Reflection. The returned {@link TableFormat} implements
     * {@link WritableTableFormat} and may be used for an editable table.
     *
     * @param baseClass the class of the Object to divide into columns. If specified,
     *      the returned class will provide implementation of
     *      {@link AdvancedTableFormat#getColumnClass(int)} and
     *      {@link AdvancedTableFormat#getColumnComparator(int)} by examining the
     *      classes of the column value.
     */
    public static TableFormat tableFormat(Class baseClass, String[] propertyNames, String[] columnLabels, boolean[] editable) {
        return new BeanTableFormat(baseClass, propertyNames, columnLabels, editable);
    }


    // TextFilterators // // // // // // // // // // // // // // // // // // //

    /**
     * Creates a {@link TextFilterator} that searches the given JavaBean
     * properties.
     */
    public static TextFilterator textFilterator(String[] propertyNames) {
        return new BeanTextFilterator(propertyNames);
    }


    // ThresholdEvaluators // // // // // // // // // // // // // // // // // //

    /**
     * Creates a {@link ThresholdEvaluator} that uses Reflection to utilize an
     * integer JavaBean property as the threshold evaluation.
     */
    public static ThresholdEvaluator thresholdEvaluator(String propertyName) {
        return new BeanThresholdEvaluator(propertyName);
    }

    // CollectionListModels   // // // // // // // // // // // // // // // // //

    /**
     * Creates a {@link CollectionListModel} that where {@link List}s or {@link EventList}s
     * are the elements of a parent {@link EventList}. This can be used to compose
     * {@link EventList}s from other {@link EventList}s.
     */
    public static CollectionListModel listCollectionListModel() {
        return new ListCollectionListModel();
    }


    // EventLists // // // // // // // // // // // // // // // // // // // // //

    /**
     * Creates a new {@link EventList} which contains the contents of the specified
     * {@link Collection}. The {@link EventList}'s order will be determined by
     * {@link Collection#iterator() contents.iterator()}.
     */
    public static EventList eventList(Collection contents) {
        BasicEventList result = new BasicEventList();
        result.addAll(contents);
        return result;
    }

    /**
     * Wraps the source in an {@link EventList} that does not allow writing operations.
     *
     * <p>The returned {@link EventList} is useful for programming defensively. A
     * {@link EventList} is useful to supply an unknown class read-only access
     * to your {@link EventList}.
     *
     * <p>The returned {@link EventList} will provides an up-to-date view of its source
     * {@link EventList} so changes to the source {@link EventList} will still be
     * reflected. For a static copy of any {@link EventList} it is necessary to copy
     * the contents of that {@link EventList} into an {@link ArrayList}.
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> This returned EventList
     * is thread ready but not thread safe. See {@link EventList} for an example
     * of thread safe code.
     */
    public static TransformedList readOnlyList(EventList source) {
        return new ReadOnlyList(source);
    }

    /**
     * Wraps the source in an {@link EventList} that obtains a
     * {@link ca.odell.glazedlists.util.concurrent.ReadWriteLock ReadWritLock} for all
     * operations.
     *
     * <p>This provides some support for sharing {@link EventList}s between multiple
     * threads.
     *
     * <p>Using a {@link ThreadSafeList} for concurrent access to lists can be expensive
     * because a {@link ca.odell.glazedlists.util.concurrent.ReadWriteLock ReadWriteLock}
     * is aquired and released for every operation.
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> Although this class
     * provides thread safe access, it does not provide any guarantees that changes
     * will not happen between method calls. For example, the following code is unsafe
     * because the source {@link EventList} may change between calls to
     * {@link TransformedList#size() size()} and {@link TransformedList#get(int) get()}:
     * <pre> EventList source = ...
     * ThreadSafeList myList = new ThreadSafeList(source);
     * if(myList.size() > 3) {
     *   System.out.println(myList.get(3));
     * }</pre>
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> The objects returned
     * by {@link TransformedList#iterator() iterator()},
     * {@link TransformedList#subList(int,int) subList()}, etc. are not thread safe.
     *
     * @see ca.odell.glazedlists.util.concurrent
     */
    public static TransformedList threadSafeList(EventList source) {
        return new ThreadSafeList(source);
    }

    /**
     * Provides a proxy to another ListEventListener that may go out of
     * scope without explicitly removing itself from the source list's set of
     * listeners.
     *
     * <p>This exists to solve a garbage collection problem. Suppose I have an
     * {@link EventList} <i>L</i> and I request a {@link ListIterator} for <i>L</i>.
     * The {@link ListIterator} must listen for change events to <i>L</i> in order
     * to be consistent. Therefore such an iterator will register
     * itself as a listener for <i>L</i>. When the iterator goes out of scope (as
     * they usually do), it will remain as a listener for <i>L</i>. This prevents
     * the iterator object from ever being garbage collected! But the iterator is
     * never used again. Because iterators can be used very frequently, this will
     * cause an unacceptable memory leak.
     *
     * <p>Instead of adding the iterator directly as a listener for <i>L</i>, add
     * the proxy instead. The proxy will retain a <code>WeakReference</code> to the
     * iterator and forward events to the iterator as long as it is reachable. When
     * theiterator is no longer reachable, the proxy will remove itself from the list
     * of listeners for <i>L</i>. All garbage is then available for collection.
     *
     * @see java.lang.ref.WeakReference
     */
    public static ListEventListener weakReferenceProxy(EventList source, ListEventListener target) {
        return new WeakReferenceProxy(source, target);
    }

    // ObservableElementList Connectors // // // // // // // // // // // // //

    /**
     * Create a new Connector for the {@link ObservableElementList} that works with
     * JavaBeans' {@link java.beans.PropertyChangeListener}. The methods to add
     * and remove listeners are detected automatically by examining the bean class
     * and searching for a method prefixed with "add" or "remove" taking a single
     * {@link java.beans.PropertyChangeListener} argument.
     *
     *
     * @param beanClass a class with both <code>addPropertyChangeListener(PropertyChangeListener)</code>
     *      and <code>removePropertyChangeListener(PropertyChangeListener)</code>,
     *      or similar methods.
     * @return an ObservableElementList.Connector for the specified class
     */
    public static ObservableElementList.Connector beanConnector(Class beanClass) {
        return new JavaBeanEventListConnector(beanClass);
    }

    /**
     * Create a new Connector for the {@link ObservableElementList} that works with
     * JavaBeans' {@link java.beans.PropertyChangeListener}. The methods to add
     * and remove listeners are specified by name. Such methods must take a single
     * {@link java.beans.PropertyChangeListener} argument.
     *
     * @param beanClass a class with both methods as specified.
     * @param addListener a method name such as "addPropertyChangeListener"
     * @param removeListener a method name such as "removePropertyChangeListener"
     * @return an ObservableElementList.Connector for the specified class
     */
    public static ObservableElementList.Connector beanConnector(Class beanClass, String addListener, String removeListener) {
        return new JavaBeanEventListConnector(beanClass, addListener, removeListener);
    }

    // Matchers // // // // // // // // // // // // // // // // // // // // // // //

    /**
     * Get a {@link MatcherEditor} that is fixed on the specified {@link Matcher}.
     */
    public static MatcherEditor fixedMatcherEditor(Matcher matcher) {
        return new FixedMatcherEditor(matcher);
    }
}