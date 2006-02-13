/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package com.publicobject.issuesbrowser.swing;

// demo
import ca.odell.glazedlists.*;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.*;
import com.publicobject.issuesbrowser.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.ConnectException;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An IssueBrowser is a program for finding and viewing issues.
 *
 * @author <a href="mailto:jesse@odel.on.ca">Jesse Wilson</a>
 */
public class IssuesBrowser implements Runnable {

    /** these don't belong here at all */
    public static final Color GLAZED_LISTS_DARK_BROWN = new Color(36, 23, 10);
    public static final Color GLAZED_LISTS_MEDIUM_BROWN = new Color(69, 64, 56);
    public static final Color GLAZED_LISTS_MEDIUM_LIGHT_BROWN = new Color(150, 140, 130);
    public static final Color GLAZED_LISTS_LIGHT_BROWN = new Color(246, 237, 220);

    /** for displaying dates */
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");

    /** an event list to host the issues */
    private EventList<Issue> issuesEventList = new BasicEventList<Issue>();

    /** all the filters currently applied to the issues list */
    private FilterPanel filterPanel = new FilterPanel(issuesEventList);

    /** the currently selected issues */
    private EventSelectionModel issuesSelectionModel = null;

    private TableModel issuesTableModel = null;

    private Issue descriptionIssue = null;

    private IssueDetailsComponent issueDetails;

    /** monitor loading the issues */
    private JLabel throbber = null;
    private ImageIcon throbberActive = null;
    private ImageIcon throbberStatic = null;

    /** a label to display the count of issues in the issue table */
    private IssueCounterLabel issueCounter = null;

    /** loads issues as requested */
    private IssueLoader issueLoader = new IssueLoader(issuesEventList, new IndeterminateToggler());

    /** the application window */
    private JFrame frame;

    /**
     * Loads the issues browser as standalone application.
     */
    public void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // do nothing - fall back to default look and feel
        }

        constructStandalone();

        // debug a problem where the thread is getting interrupted
        if (Thread.currentThread().isInterrupted()) {
            new Exception("thread has been interrupted").printStackTrace();
        }

        // we have advice for the user when we cannot connect to a host
        Exceptions.getInstance().addHandler(new UnknownHostExceptionHandler());
        Exceptions.getInstance().addHandler(new ConnectExceptionHandler());
        Exceptions.getInstance().addHandler(new NoRouteToHostExceptionHandler());
        Exceptions.getInstance().addHandler(new AccessControlExceptionHandler());

        // start loading the issues
        issueLoader.start();
    }

    /**
     * Constructs the browser as a standalone frame.
     */
    private void constructStandalone() {
        // create a frame with that panel
        frame = new JFrame("Issues");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setLayout(new GridBagLayout());
        frame.getContentPane().add(constructView(), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        frame.setVisible(true);
    }

    /**
     * Display a frame for browsing issues.
     */
    private JPanel constructView() {
        // sort the original issues list
        final SortedList<Issue> issuesSortedList = new SortedList<Issue>(issuesEventList, null);

        // filter the sorted issues
        FilterList<Issue> filteredIssues = new FilterList<Issue>(issuesSortedList, filterPanel.getMatcherEditor());

        SeparatorList<Issue> separatedIssues = new SeparatorList<Issue>(filteredIssues, GlazedLists.beanPropertyComparator(Issue.class, "subcomponent"), 0, Integer.MAX_VALUE);


        // build the issues table
        issuesTableModel = new EventTableModel<Issue>(separatedIssues, new IssueTableFormat());
        JTable issuesJTable = new JTable(issuesTableModel);
        issuesSelectionModel = new EventSelectionModel<Issue>(separatedIssues);
        issuesSelectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE); // multi-selection best demos our awesome selection management
        issuesSelectionModel.addListSelectionListener(new IssuesSelectionListener());
        issuesJTable.setSelectionModel(issuesSelectionModel);
        issuesJTable.getColumnModel().getColumn(0).setPreferredWidth(10);
        issuesJTable.getColumnModel().getColumn(1).setPreferredWidth(30);
        issuesJTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        issuesJTable.getColumnModel().getColumn(3).setPreferredWidth(30);
        issuesJTable.getColumnModel().getColumn(4).setPreferredWidth(30);
        issuesJTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        // turn off cell focus painting
        issuesJTable.setDefaultRenderer(String.class, new NoFocusRenderer(issuesJTable.getDefaultRenderer(String.class)));
        issuesJTable.setDefaultRenderer(Integer.class, new NoFocusRenderer(issuesJTable.getDefaultRenderer(Integer.class)));
        issuesJTable.setDefaultRenderer(Priority.class, new NoFocusRenderer(new PriorityTableCellRenderer()));
        new TableComparatorChooser<Issue>(issuesJTable, issuesSortedList, TableComparatorChooser.MULTIPLE_COLUMN_KEYBOARD);
        JScrollPane issuesTableScrollPane = new JScrollPane(issuesJTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        issuesTableScrollPane.getViewport().setBackground(UIManager.getColor("EditorPane.background"));
        issuesTableScrollPane.setBorder(BorderFactory.createEmptyBorder());

        issueDetails = new IssueDetailsComponent(filteredIssues);

        // projects
        EventList<Project> projects = Project.getProjects();

        // project select combobox
        EventComboBoxModel projectsComboModel = new EventComboBoxModel<Project>(projects);
        JComboBox projectsCombo = new JComboBox(projectsComboModel);
        projectsCombo.setEditable(false);
        projectsCombo.setOpaque(false);
        projectsCombo.addItemListener(new ProjectChangeListener());
        projectsComboModel.setSelectedItem(new Project(null, "Select a Java.net project..."));

        // build a label to display the number of issues in the issue table
        issueCounter = new IssueCounterLabel(filteredIssues);
        issueCounter.setHorizontalAlignment(SwingConstants.CENTER);
        issueCounter.setForeground(Color.WHITE);

        // throbber icons
        ClassLoader jarLoader = IssuesBrowser.class.getClassLoader();
        URL url = jarLoader.getResource("resources/throbber-static.gif");
        if (url != null) throbberStatic = new ImageIcon(url);
        url = jarLoader.getResource("resources/throbber-active.gif");
        if (url != null) throbberActive = new ImageIcon(url);
        throbber = new JLabel(throbberStatic);
        throbber.setHorizontalAlignment(SwingConstants.RIGHT);

        // header bar
        JPanel iconBar = new GradientPanel(GLAZED_LISTS_MEDIUM_BROWN, GLAZED_LISTS_DARK_BROWN, true);
        iconBar.setLayout(new GridLayout(1, 3));
        iconBar.add(projectsCombo);
        iconBar.add(issueCounter);
        iconBar.add(throbber);
        iconBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // assemble all data components on a common panel
        JPanel dataPanel = new JPanel();
        JComponent issueDetailsComponent = issueDetails;
        dataPanel.setLayout(new GridLayout(2, 1));
        dataPanel.add(issuesTableScrollPane);
        dataPanel.add(issueDetailsComponent);

        // draw lines between components
        JComponent filtersPanel = filterPanel.getComponent();
        filtersPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, IssuesBrowser.GLAZED_LISTS_DARK_BROWN));
        issueDetailsComponent.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GLAZED_LISTS_DARK_BROWN));

        // the outermost panel to layout the icon bar with the other panels
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.add(iconBar,                        new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(filtersPanel,                   new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(Box.createHorizontalStrut(250), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(dataPanel,                      new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        return mainPanel;
    }

    /**
     * Listens for changes in the selection on the issues table.
     */
    class IssuesSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            // get the newly selected issue
            Issue selected = null;
            if(issuesSelectionModel.getSelected().size() > 0)
                selected = (Issue)issuesSelectionModel.getSelected().get(0);

            // update the description issue
            if(selected == descriptionIssue) return;
            descriptionIssue = selected;
            issueDetails.setIssue(descriptionIssue);
        }
    }

    /**
     * Listens for changes to the project combo box and updates which project is
     * being loaded.
     */
    class ProjectChangeListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() != ItemEvent.SELECTED) return;

            final Project selectedProject = (Project) e.getItem();
            if(selectedProject.isValid()) issueLoader.setProject(selectedProject);
        }
    }

    /**
     * When started via a main method, this creates a standalone issues browser.
     */
    public static void main(String[] args) {
        // load the issues and display the browser
        IssuesBrowser browser = new IssuesBrowser();
        SwingUtilities.invokeLater(browser);
    }

    /**
     * Toggles the throbber on and off.
     */
    private class IndeterminateToggler implements Runnable, Throbber {

        /** whether the throbber will be turned on or off */
        private boolean on = false;

        public synchronized void setOn() {
            if (!on) {
                on = true;
                SwingUtilities.invokeLater(this);
            }
        }

        public synchronized void setOff() {
            if (on) {
                on = false;
                SwingUtilities.invokeLater(this);
            }
        }

        public synchronized void run() {
            if(on) throbber.setIcon(throbberActive);
            else throbber.setIcon(throbberStatic);
        }
    }


    /**
     * A custom label designed for displaying the number of issues in the issue
     * table. Use {@link #setIssueCount(int)} to update the text of the label
     * to reflect a new issue count.
     */
    private static class IssueCounterLabel extends JLabel implements ListEventListener<Issue> {
        private static final MessageFormat issueCountFormat = new MessageFormat("{0} {0,choice,0#issues|1#issue|1<issues}");

        private int issueCount = -1;

        public IssueCounterLabel(EventList<Issue> source) {
            source.addListEventListener(this);
            this.setIssueCount(source.size());
        }

        public void setIssueCount(int issueCount) {
            if (this.issueCount == issueCount) return;
            this.issueCount = issueCount;
            this.setText(issueCountFormat.format(new Object[] {new Integer(issueCount)}));
        }
        public void listChanged(ListEvent<Issue> listChanges) {
            setIssueCount(listChanges.getSourceList().size());
        }
    }

    /**
     * A customized panel which paints a color gradient for its background
     * rather than a single color. The start and end colors of the gradient
     * are specified via the constructor.
     */
    public static class GradientPanel extends JPanel {
        private Color gradientStartColor;
        private Color gradientEndColor;
        private boolean vertical;

        public GradientPanel(Color gradientStartColor, Color gradientEndColor, boolean vertical) {
            this.gradientStartColor = gradientStartColor;
            this.gradientEndColor = gradientEndColor;
            this.vertical = vertical;
        }

        public void paintComponent(Graphics g) {
            if (this.isOpaque())
                paintGradient((Graphics2D) g, this.gradientStartColor, this.gradientEndColor, vertical ? this.getHeight() : this.getWidth(), vertical);
        }
    }

    /**
     * A convenience method to paint a gradient between <code>gradientStartColor</code>
     * and <code>gradientEndColor</code> over <code>length</code> pixels.
     */
    private static void paintGradient(Graphics2D g2d, Color gradientStartColor, Color gradientEndColor, int length, boolean vertical) {
        final Paint oldPainter = g2d.getPaint();
        try {
            if(vertical) g2d.setPaint(new GradientPaint(0, 0, gradientStartColor, 0, length, gradientEndColor));
            else g2d.setPaint(new GradientPaint(0, 0, gradientStartColor, length, 0, gradientEndColor));
            g2d.fill(g2d.getClip());
        } finally {
            g2d.setPaint(oldPainter);
        }
    }

    /**
     * An abstract Exceptions.Handler for all types of Exceptions that indicate
     * a connection to the internet could not be establishedd. It displays an
     * informative message stating how to configure Java to use a proxy
     * server.
     */
    private abstract class AbstractCannotConnectExceptionHandler implements Exceptions.Handler {
        public void handle(Exception e) {
            final String title = "Unable to connect to the Internet";

            final String message;
            final String osname = System.getProperty("os.name");
            if (osname != null && osname.toLowerCase().contains("windows")) {
                // explain how to configure a Proxy Server for Java on Windows
                message = "If connecting to the Internet via a proxy server,\n" +
                          "ensure you have configured Java correctly in\n" +
                          "Control Panel \u2192 Java \u2192 General \u2192 Network Settings...\n\n" +
                          "You must restart this application if you adjust the settings.";
            } else {
                message = "Please check your Internet connection settings.";
            }

            SwingUtilities.invokeLater(new ShowMessageDialogRunnable(title, message));
        }
    }

    /**
     * An Exceptions.Handler for UnknownHostExceptions that displays an
     * informative message stating how to configure Java to use a proxy
     * server.
     */
    private class UnknownHostExceptionHandler extends AbstractCannotConnectExceptionHandler {
        public boolean recognize(Exception e) {
            return e instanceof UnknownHostException;
        }
    }

    /**
     * An Exceptions.Handler for ConnectExceptions that displays an
     * informative message stating how to configure Java to use a proxy
     * server.
     */
    private class ConnectExceptionHandler extends AbstractCannotConnectExceptionHandler {
        public boolean recognize(Exception e) {
            return e instanceof ConnectException;
        }
    }

    /**
     * An Exceptions.Handler for NoRouteToHostException that displays an
     * informative message stating the probable cause and how to configure
     * Java to use a proxy server.
     */
    private class NoRouteToHostExceptionHandler implements Exceptions.Handler {
        public boolean recognize(Exception e) {
            return e instanceof NoRouteToHostException;
        }

        public void handle(Exception e) {
            final String title = "Unable to find a route to the Host";

            final String message;
            final String osname = System.getProperty("os.name");
            if (osname != null && osname.toLowerCase().contains("windows")) {
                // explain how to configure a Proxy Server for Java on Windows
                message = "Typically, the remote host cannot be reached because of an\n" +
                          "intervening firewall, or if an intermediate router is down.\n\n" +
                          "If connecting to the Internet via a proxy server,\n" +
                          "ensure you have configured Java correctly in\n" +
                          "Control Panel \u2192 Java \u2192 General \u2192 Network Settings...\n\n" +
                          "You must restart this application if you adjust the settings.";
            } else {
                message = "Please check your Internet connection settings.";
            }

            // explain how to configure a Proxy Server for Java on Windows
            SwingUtilities.invokeLater(new ShowMessageDialogRunnable(title, message));
        }
    }

    /**
     * An Exceptions.Handler for an AccessControlException when attempting to resolve
     * a hostname to an IP address that displays an informative message stating the
     * probable cause and how to configure Java to use a proxy server.
     */
    private class AccessControlExceptionHandler implements Exceptions.Handler {
        // sample message: "access denied (java.net.SocketPermission javacc.dev.java.net resolve)"
        private final Matcher messageMatcher = Pattern.compile(".*access denied \\p{Punct}java.net.SocketPermission (.*) resolve\\p{Punct}").matcher("");

        public boolean recognize(Exception e) {
            return e instanceof AccessControlException && messageMatcher.reset(e.getMessage()).matches();
        }

        public void handle(Exception e) {
            final String title = "Unable to resolve the address of the Host";

            final String message;
            final String osname = System.getProperty("os.name");
            if (osname != null && osname.toLowerCase().contains("windows")) {
                // explain how to configure a Proxy Server for Java on Windows
                message = "If connecting to the Internet via a proxy server,\n" +
                          "ensure you have configured Java correctly in\n" +
                          "Control Panel \u2192 Java \u2192 General \u2192 Network Settings...\n\n" +
                          "You must restart this application if you adjust the settings.";
            } else {
                message = "Please check your Internet connection settings.";
            }

            // explain how to configure a Proxy Server for Java on Windows
            SwingUtilities.invokeLater(new ShowMessageDialogRunnable(title, message));
        }
    }

    /**
     * A convenience class to show a message dialog to the user.
     */
    private class ShowMessageDialogRunnable implements Runnable {
        private final String title;
        private final String message;

        public ShowMessageDialogRunnable(String title, String message) {
            this.title = title;
            this.message = message;
        }

        public void run() {
            JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);
        }
    }
}