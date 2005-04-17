/*             Glazed Lists  http://publicobject.com/glazedlists/             */                        
/*        Copyright 2003-2005 publicobject.com, O'Dell Engineering Ltd.       */
package ca.odell.glazedlists.swing;

// the core Glazed Lists packages
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.event.*;
// Swing toolkit stuff for displaying widgets
import javax.swing.*;
// for responding to user actions
import java.awt.event.*;
import javax.swing.event.*;


/**
 * A combo box model for displaying Glazed Lists in a combo box.
 *
 * <p>The implementation of setSelection and getSelection is not in any way tied
 * to the contents of the list.
 *
 * @see <a href="http://publicobject.com/glazedlists/tutorial-0.9.1/">Glazed
 * Lists Tutorial</a>
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class EventComboBoxModel extends EventListModel implements ComboBoxModel {

    /** the currently selected item, should belong to the source list */
    private Object selected;
    
    /**
     * Creates a new combo box model that displays the specified source list
     * in the combo box.
     */
    public EventComboBoxModel(EventList source) {
        super(source);
    }
    
    /**
     * Gets the currently selected item.
     */
    public Object getSelectedItem() {
        return selected;
    }

    /**
     * Sets the currently selected item.
     *
     * <p>The selection notification process is very much a hack. This fires
     * a ListDataEvent where the range is between -1 and -1. This is identical
     * to the notification process used by the {@link DefaultComboBoxModel}.
     */
    public void setSelectedItem(Object selected) {
        this.selected = selected;
        listDataEvent.setRange(-1, -1);
        listDataEvent.setType(ListDataEvent.CONTENTS_CHANGED);
        fireListDataEvent(listDataEvent);
    }
}
