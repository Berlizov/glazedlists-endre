/**
 * Glazed Lists
 * http://glazedlists.dev.java.net/
 *
 * COPYRIGHT 2003 O'DELL ENGINEERING LTD.
 */
package ca.odell.glazedlists.swt;

// the core Glazed Lists packages
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.gui.*;
import ca.odell.glazedlists.event.*;
// SWT toolkit stuff for displaying widgets
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
// standard collections as support
import java.util.*;

/**
 * A helper that displays an EventList in an SWT table.
 *
 * <p>This class is not thread safe. It must be used exclusively with the SWT
 * event handler thread.
 *
 * <p><strong>Warning:</strong> This class is a a developer preview and subject to
 * many bugs and API changes.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class EventTableViewer implements ListEventListener {

    /** the heavyweight table */
    private Table table;

    /** whether the underlying table is Virtual */
    private boolean tableIsVirtual = false;

    /** the complete list of messages before filters */
    protected EventList source;

    /** Specifies how to render table headers and sort */
    private TableFormat tableFormat;

    /** Enables check support */
    private TableCheckFilterList checkFilter = null;

    /**
     * Creates a new table that renders the specified list in the specified format.
     */
    public EventTableViewer(EventList source, Table table, TableFormat tableFormat) {
        // insert a checked source if supported by the table
        if((table.getStyle() & SWT.CHECK) > 0) {
            this.checkFilter = new TableCheckFilterList(source, table, tableFormat);
            source = checkFilter;
        }

        // save table, source list and table format
        this.table = table;
        this.source = source;
        this.tableFormat = tableFormat;

        // determine if the provided table is Virtual
        tableIsVirtual = SWT.VIRTUAL == (table.getStyle() & SWT.VIRTUAL);

        initTable();
        if(!tableIsVirtual) {
            populateTable();
        } else {
            table.setItemCount(source.size());
            table.addListener(SWT.SetData, new VirtualTableListener());
        }

        // listen for events, using the user interface thread
        if(source == checkFilter) {
            source.addListEventListener(this);
        } else {
            source.addListEventListener(new UserInterfaceThreadProxy(this, table.getDisplay()));
        }
    }

    /**
     * Builds the tables columns and table headers
     */
    private void initTable() {
        table.setHeaderVisible(true);
        for(int c = 0; c < tableFormat.getColumnCount(); c++) {
            TableColumn column = new TableColumn(table, SWT.LEFT, c);
            column.setText((String)tableFormat.getColumnName(c));
            column.setWidth(80);
        }
    }

    /**
     * Populates the table with the initial data from the list.
     */
    private void populateTable() {
        for(int r = 0; r < source.size(); r++) {
            addRow(r, source.get(r));
        }
    }

    /**
     * Adds the item at the specified row.
     */
    private void addRow(int row, Object value) {
        // Table isn't Virtual, or adding in the middle
        if(!tableIsVirtual || row < table.getItemCount()) {
            TableItem item = new TableItem(table, 0, row);
            setItemText(item, value);

        // Table is Virtual and adding at the end
        } else {
            table.setItemCount(table.getItemCount() + 1);
        }
    }

    /**
     * Updates the item at the specified row.
     */
    private void updateRow(int row, Object value) {
        TableItem item = table.getItem(row);
        setItemText(item, value);
    }

    /**
     * Sets all of the column values on a TableItem.
     */
    private void setItemText(TableItem item, Object value) {
        for(int i = 0; i < tableFormat.getColumnCount(); i++) {
            Object cellValue = tableFormat.getColumnValue(value, i);
            if(cellValue != null) item.setText(i, cellValue.toString());
            else item.setText(i, "");
        }
    }

    /**
     * Gets the Table Format.
     */
    public TableFormat getTableFormat() {
        return tableFormat;
    }

    /**
     * Gets the table being managed by this {@link EventTableViewer}.
     */
    public Table getTable() {
        return table;
    }


    /**
     * Sets this table to be rendered by a different table format.
     */
    public void setTableFormat(TableFormat tableFormat) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set whether this shall show only checked elements.
     */
    public void setCheckedOnly(boolean checkedOnly) {
        checkFilter.setCheckedOnly(checkedOnly);
    }
    /**
     * Get whether this is showing only checked elements.
     */
    public boolean getCheckedOnly() {
        return checkFilter.getCheckedOnly();
    }

    /**
     * Gets all checked items.
     */
    public List getAllChecked() {
        return checkFilter.getAllChecked();
    }

    /**
     * Get the source of this {@link EventTableViewer}.
     */
    public EventList getSourceList() {
        return source;
    }


    /**
     * When the source list is changed, this forwards the change to the
     * displayed table.
     *
     * <p>This implementation saves the entire table's selection in an ArrayList before
     * walking through the list of changes. It then walks through the table's changes.
     * Finally it adjusts the selection on the table in response to the changes.
     * Although simple, this implementation has much higher memory and runtime
     * requirements than necessary. It is desirable to optimize this method by
     * not storing a second copy of the selection list. Such an implementation would
     * use only the selection data available in the table plus a list of entries
     * which have been since overwritten.
     */
    public void listChanged(ListEvent listChanges) {
        source.getReadWriteLock().readLock().lock();
        try {

            // save the former selection
            List selection = new ArrayList();
            for(int i = 0; i < table.getItemCount(); i++) {
                selection.add(i, Boolean.valueOf(table.isSelected(i)));
            }

            // walk the list
            while(listChanges.next()) {
                int changeIndex = listChanges.getIndex();
                int changeType = listChanges.getType();

                if(changeType == ListEvent.INSERT) {
                    selection.add(changeIndex, Boolean.FALSE);
                    addRow(changeIndex, source.get(changeIndex));
                } else if(changeType == ListEvent.UPDATE) {
                    updateRow(changeIndex, source.get(changeIndex));
                } else if(changeType == ListEvent.DELETE) {
                    selection.remove(changeIndex);
                    table.remove(changeIndex);
                }
            }

            // apply the saved selection
            for(int i = 0; i < table.getItemCount(); i++) {
                boolean selected = ((Boolean)selection.get(i)).booleanValue();
                if(selected) {
                    table.select(i);
                } else {
                    table.deselect(i);
                }
            }
        } finally {
            source.getReadWriteLock().readLock().unlock();
        }
    }

    /**
     * Respond to view changes on a Virtual Table
     */
    protected final class VirtualTableListener implements Listener {
        public void handleEvent(Event e) {
            TableItem item = (TableItem)e.item;
            int tableIndex = table.indexOf(item);
            Object value = source.get(tableIndex);
            setItemText(item, value);
        }
    }
}
