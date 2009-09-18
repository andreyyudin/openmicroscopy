/*
 * ome.formats.importer.gui.GuiImporter
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *------------------------------------------------------------------------------
 */

package ome.formats.importer.gui;

import ome.formats.importer.util.ErrorHandler.*;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.Version;
import ome.formats.importer.util.BareBonesBrowserLaunch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmicroscopy.shoola.util.ui.MacOSMenuHandler;
import org.openmicroscopy.shoola.util.ui.login.LoginCredentials;
import org.openmicroscopy.shoola.util.ui.login.ScreenLogin;
import org.openmicroscopy.shoola.util.ui.login.ScreenLogo;

/**
 * @author Brian W. Loranger
 */

public class GuiImporter extends JFrame 
    implements  ActionListener, WindowListener, IObserver, PropertyChangeListener, 
                WindowStateListener, WindowFocusListener
{
    private static final long   serialVersionUID = 1228000122345370913L;

    private static final String show_log_file = "show_log_file_location";
    
    private static Log          log     = LogFactory.getLog(GuiImporter.class);
   
    // -- Constants --
    private final static boolean useSplashScreenAbout   = false;
    static boolean USE_QUAQUA = false;
    
    
    public final static String TITLE            = "OMERO.importer";
    
    public final static String splash           = "gfx/importer_splash.png";
    public static final String ICON = "gfx/icon.png";
    public static final String QUIT_ICON = "gfx/nuvola_exit16.png";
    public static final String LOGIN_ICON = "gfx/nuvola_login16.png";
    public static final String COMMENT_ICON = "gfx/nuvola_sendcomment16.png";
    public static final String HOME_ICON = "gfx/nuvola_home16.png";
    public static final String ABOUT_ICON = "gfx/nuvola_about16.png";
    public static final String HISTORY_ICON = "gfx/nuvola_history16.png";
    public static final String CHOOSER_ICON = "gfx/nuvola_chooser16.png";
    public static final String OUTPUT_ICON = "gfx/nuvola_output16.png";
    public static final String BUG_ICON = "gfx/nuvola_bug16.png";
    public static final String CONFIG_ICON = "gfx/nuvola_configure16.png";
    public static final String ERROR_ICON_ANIM = "gfx/warning_msg16_anim.gif";
    public static final String ERROR_ICON = "gfx/warning_msg16.png";
    public static final String LOGFILE_ICON = "gfx/nuvola_output16.png";
    
    public final ImportConfig         config;
    public final GuiCommonElements    gui;
    public final ErrorHandler         errorHandler;
    public final FileQueueHandler     fileQueueHandler;
    public final StatusBar            statusBar;

    public LoginHandler         loginHandler;
    public HistoryHandler       historyHandler;
    public HistoryTable         historyTable;
    
    public ScreenLogin          view;
    public ScreenLogo           viewTop;

    private JMenuBar            menubar;
    private JMenu               fileMenu;
    private JMenuItem           fileQuit;
    private JMenuItem           login;
    private JMenu               helpMenu;
    private JMenuItem           helpComment;
    private JMenuItem           helpHome;
    private JMenuItem           helpAbout;
    
    public Boolean              loggedIn;

    private JTextPane           outputTextPane;
    private JTextPane           debugTextPane;
    private JPanel              historyPanel;
    private JPanel              errorPanel;
    
    private JTabbedPane         tPane;


    
    /**
     * Main entry class for the application
     */
    public GuiImporter(ImportConfig config)
    {
        //super(TITLE);
        
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(0);

        this.config = config;
        this.gui = new GuiCommonElements(config);
        

        // Add a shutdown hook for when app closes
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() { shutdown(); }
        });
        
        // Set app defaults
        setTitle(config.getAppTitle());
        setIconImage(gui.getImageIcon(GuiImporter.ICON).getImage());
        setPreferredSize(new Dimension(gui.bounds.width, gui.bounds.height));
        setSize(gui.bounds.width, gui.bounds.height);
        setLocation(gui.bounds.x, gui.bounds.y);      
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pack();

        addWindowListener(this);

        // capture move info
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent evt) {
                gui.bounds = getBounds();
            }
        });

        // capture resize info
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                gui.bounds = getBounds();               
            }
        });
        
        // menu bar
        menubar = new JMenuBar();
        fileMenu = new JMenu("File");
        menubar.add(fileMenu);
        login = new JMenuItem("Login to the server...", gui.getImageIcon(LOGIN_ICON));
        login.setActionCommand("login");
        login.addActionListener(this);        
        fileMenu.add(login);
        /*
        if (gui.getIsMac())
        {
            options = new JMenuItem("Options...", gui.getImageIcon(CONFIG_ICON));
            options.setActionCommand("options");
            options.addActionListener(this);        
            fileMenu.add(options);
        }
        */
        fileQuit = new JMenuItem("Quit", gui.getImageIcon(QUIT_ICON));
        fileQuit.setActionCommand("quit");
        fileQuit.addActionListener(this);
        fileMenu.add(fileQuit);
        helpMenu = new JMenu("Help");
        menubar.add(helpMenu);
        helpComment = new JMenuItem("Send a Comment...", gui.getImageIcon(COMMENT_ICON));
        helpComment.setActionCommand("comment");
        helpComment.addActionListener(this);
        helpHome = new JMenuItem("Visit Importer Homepage...", gui.getImageIcon(HOME_ICON));
        helpHome.setActionCommand("home");
        helpHome.addActionListener(this);
        helpAbout = new JMenuItem("About the Importer...", gui.getImageIcon(ABOUT_ICON));
        helpAbout.setActionCommand("about");
        helpAbout.addActionListener(this);
        helpMenu.add(helpComment);
        helpMenu.add(helpHome);
        // Help --> Show log file location...
        JMenuItem helpShowLog = new JMenuItem("Show log file location...", gui.getImageIcon(LOGFILE_ICON));
        helpShowLog.setActionCommand(show_log_file);
        helpShowLog.addActionListener(this);
        helpMenu.add(helpShowLog);
        helpMenu.add(helpAbout);
        // Help --> About
        setJMenuBar(menubar);
      
        // tabbed panes
        tPane = new JTabbedPane();
        tPane.setOpaque(false); // content panes must be opaque

        // file chooser pane
        JPanel filePanel = new JPanel(new BorderLayout());

        // The file chooser sub-pane
        fileQueueHandler = new FileQueueHandler(this, config);
        //splitPane.setResizeWeight(0.5);

        filePanel.add(fileQueueHandler, BorderLayout.CENTER);
        tPane.addTab("File Chooser", gui.getImageIcon(CHOOSER_ICON), filePanel,
        "Add and delete images here to the import queue.");
        tPane.setMnemonicAt(0, KeyEvent.VK_1);

        // history pane
        historyPanel = new JPanel();
        historyPanel.setOpaque(false);
        historyPanel.setLayout(new BorderLayout());
        
        tPane.addTab("Import History", gui.getImageIcon(HISTORY_ICON), historyPanel,
                "Import history is displayed here.");
        tPane.setMnemonicAt(0, KeyEvent.VK_4);
       
        // output text pane
        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BorderLayout());
        outputTextPane = new JTextPane();
        outputTextPane.setEditable(false);

        JScrollPane outputScrollPane = new JScrollPane();
        outputScrollPane.getViewport().add(outputTextPane);
        
        outputScrollPane.getVerticalScrollBar().addAdjustmentListener(
                new AdjustmentListener()
        {
            public void adjustmentValueChanged(AdjustmentEvent e)
            {
                outputTextPane.setCaretPosition(outputTextPane.getDocument().
                        getLength());
            }
        }
        );

        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        tPane.addTab("Output Text", gui.getImageIcon(OUTPUT_ICON), outputPanel,
                "Standard output text goes here.");
        tPane.setMnemonicAt(0, KeyEvent.VK_2);


        // debug pane
        JPanel debugPanel = new JPanel();
        debugPanel.setLayout(new BorderLayout());
        debugTextPane = new JTextPane();
        debugTextPane.setEditable(false);

        JScrollPane debugScrollPane = new JScrollPane();
        debugScrollPane.getViewport().add(debugTextPane);

        debugScrollPane.getVerticalScrollBar().addAdjustmentListener(
                new AdjustmentListener()
        {
            public void adjustmentValueChanged(AdjustmentEvent e)
            {
                debugTextPane.setCaretPosition(debugTextPane.getDocument().
                        getLength());
            }
        }
        );

        debugPanel.add(debugScrollPane, BorderLayout.CENTER);

        tPane.addTab("Debug Text", gui.getImageIcon(BUG_ICON), debugPanel,
                "Debug messages are displayed here.");
        tPane.setMnemonicAt(0, KeyEvent.VK_3);

        // Error Pane
        errorPanel = new JPanel();
        errorPanel.setOpaque(false);
        errorPanel.setLayout(new BorderLayout());

        tPane.addTab("Import Errors", gui.getImageIcon(ERROR_ICON), errorPanel,
        "Import errors are displayed here.");
        tPane.setMnemonicAt(0, KeyEvent.VK_5);
        
        tPane.setSelectedIndex(0);
        
        // Add the tabbed pane to this panel.
        add(tPane);
        
        statusBar = new StatusBar();
        statusBar.setStatusIcon("gfx/server_disconn16.png",
                "Server disconnected.");
        statusBar.setProgress(false, 0, "");
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);

        this.setVisible(false);

        try {
            historyHandler = new HistoryHandler(this);
            historyPanel.add(historyHandler, BorderLayout.CENTER);
            historyTable = historyHandler.table;
        } catch (Exception e) {
            log.debug("Disabling history");
        }
        
        loginHandler = new LoginHandler(this, historyTable);
        
        LogAppender.getInstance().setTextArea(debugTextPane);
        appendToOutputLn("> Starting the importer (revision "
                + getPrintableKeyword(Version.revision) + ").");
        appendToOutputLn("> Build date: " + getPrintableKeyword(Version.revisionDate));
        appendToOutputLn("> Release date: " + Version.releaseDate);

        errorHandler = new ErrorHandler(config);
        errorHandler.addObserver(this);
        errorPanel.add(errorHandler, BorderLayout.CENTER);
        
        macMenuFix();
        
        //displayLoginDialog(this, true);
    }
    
    // save ini file and gui settings on exist
    protected void shutdown()
    {
        // Get and save the UI window placement
        try {
            config.setUIBounds(gui.getUIBounds());
        } finally {
            config.saveAll();
        }
    }
    
    /* Fixes menu issues with the about this app quit functions on mac */
    private void macMenuFix()
    {
        try {

            MacOSMenuHandler handler = new MacOSMenuHandler(this);

            handler.initialize();

            addPropertyChangeListener(this);

        } catch (Throwable e) {}
    }
    
    public boolean displayLoginDialog(Object viewer, boolean modal, boolean displayTop)
    {   
        Image img = Toolkit.getDefaultToolkit().createImage(ICON);
        view = new ScreenLogin(config.getAppTitle(),
                gui.getImageIcon("gfx/login_background.png"),
                img,
                config.getVersionNumber(), Integer.toString(config.port.get()));
        view.showConnectionSpeed(false);
        viewTop = new ScreenLogo(config.getAppTitle(), gui.getImageIcon(splash), img);
        viewTop.setStatusVisible(false);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension d = viewTop.getExtendedSize();
        Dimension dlogin = view.getPreferredSize();
        Rectangle r;
        int totalHeight;
        if (displayTop)
        {
            totalHeight = d.height+dlogin.height;
            viewTop.setBounds((screenSize.width-d.width)/2, 
                    (screenSize.height-totalHeight)/2, 
                    d.width, viewTop.getSize().height);
            r = viewTop.getBounds();
            
            viewTop.addPropertyChangeListener((PropertyChangeListener) viewer);
            viewTop.addWindowStateListener((WindowStateListener) viewer);
            viewTop.addWindowFocusListener((WindowFocusListener) viewer); 
            view.setBounds(r.x, r.y+d.height, dlogin.width, dlogin.height);
       } else {
            totalHeight = dlogin.height;
            view.setBounds((screenSize.width-d.width)/2,
                    (screenSize.height-totalHeight)/2, 
                    dlogin.width, dlogin.height);
            view.setQuitButtonText("Canel");
        }
        view.addPropertyChangeListener((PropertyChangeListener) viewer);
        view.addWindowStateListener((WindowStateListener) viewer);
        view.addWindowFocusListener((WindowFocusListener) viewer);
        view.setAlwaysOnTop(false);
        
        
        viewTop.setVisible(displayTop);
        view.setVisible(true);
        
        return true;
    }
    
    /**
     * @param s This method appends data to the output window.
     */
    public void appendToOutput(String s)
    {
        try
        {
            StyledDocument doc = (StyledDocument) outputTextPane.getDocument();
            Style style = doc.addStyle("StyleName", null);
            StyleConstants.setForeground(style, Color.black);
            StyleConstants.setFontFamily(style, "SansSerif");
            StyleConstants.setFontSize(style, 12);
            StyleConstants.setBold(style, false);

            doc.insertString(doc.getLength(), s, style);
            
            //trim the document size so it doesn't grow to big
            int maxChars = 200000;
            if (doc.getLength() > maxChars)
                doc.remove(0, doc.getLength() - maxChars);
            
            //outputTextPane.setDocument(doc);
        } catch (BadLocationException e) {}
    }

    /**
     * @param s Append to the output window and add a line return
     */
    public void appendToOutputLn(String s)
    {
        appendToOutput(s + "\n");
    }

    /**
     * @param s This method appends data to the output window.
     */
    public void appendToDebug(String s)
    {
        log.debug(s);
        try
        {          
            StyledDocument doc = (StyledDocument) debugTextPane.getDocument();
            
            Style style = doc.addStyle("StyleName", null);
            StyleConstants.setForeground(style, Color.black);
            StyleConstants.setFontFamily(style, "SansSerif");
            StyleConstants.setFontSize(style, 12);
            StyleConstants.setBold(style, false);

            doc.insertString(doc.getLength(), s, style);
            
            //trim the document size so it doesn't grow to big
            int maxChars = 200000;
            if (doc.getLength() > maxChars)
                doc.remove(0, doc.getLength() - maxChars);
            
            //debugTextPane.setDocument(doc);
        } catch (BadLocationException e) {}
    }

    /**
     * @param s Append to the output window and add a line return
     */
    public void appendToDebugLn(String s)
    {
        appendToDebug(s + "\n");
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();

        if ("login".equals(cmd))
        {
            if (loggedIn == true)
            {
                setImportEnabled(false);
                loggedIn = false;
                appendToOutputLn("> Logged out.");
                statusBar.setStatusIcon("gfx/server_disconn16.png", "Logged out.");
                loginHandler.logout();
                loginHandler = null;
            } else 
            {
                HistoryTable table = null;
                if (historyHandler != null) {
                    table = historyHandler.table;
                }
                loginHandler = new LoginHandler(this, table, true, true);
                loginHandler.displayLogin(false);
            }
        } else if ("quit".equals(cmd)) {
            if (gui.quitConfirmed(this, null) == true)
            {
                System.exit(0);
            }
        } else if ("options".equals(cmd)) {
            OptionsDialog dialog = 
                new OptionsDialog(gui, this, "Import", true);
        }
        else if ("about".equals(cmd))
        {
            // HACK - JOptionPane prevents shutdown on dispose
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            About.show(this, config, useSplashScreenAbout);
        }
        else if ("comment".equals(cmd))
        {
            new CommentMessenger(this, "OMERO.importer Comment Dialog", config, true, true);
        }
        else if ("home".equals(cmd))
        {
            BareBonesBrowserLaunch.openURL(config.getHomeUrl());
        }
        else if (show_log_file.equals(cmd))
        {
            File path = new File(config.getUserSettingsDirectory());
            try
            {
                String url = path.toURI().toURL().toString();
                url = url.replaceAll("^file:/", "file:///");
                BareBonesBrowserLaunch.openURL(url);
            }
            catch (MalformedURLException ex)
            {
                log.error("Error while transforming URL for: " 
                        + path.getAbsolutePath(), ex);
            }
        }
        
    }

    /**
     * @param keyword
     * @return This function strips out the unwanted sections of the keywords
     *         used for the version number and build time variables, leaving
     *         only the stuff we want.
     */
    public static String getPrintableKeyword(String keyword)
    {
        int begin = keyword.indexOf(" ") + 1;
        int end = keyword.lastIndexOf(" ");
        return keyword.substring(begin, end);
    }

    /** Toggles wait cursor. */
    public void waitCursor(boolean wait)
    {
        setCursor(wait ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    /**
     * @param toggle boolean toggle for the import menu
     */
    public void setImportEnabled(boolean toggle)
    {
        if (toggle == true) login.setText("Logout of the server...");
        else login.setText("Login to the server...");
    }

    /**
     * only allow the exit menu option
     */
    public void onlyAllowExit()
    {
        fileMenu.setEnabled(true);
        helpMenu.setEnabled(true);
    }

    /**
     * @param toggle Enable all menu options
     */
    public void enableMenus(boolean toggle)
    {
        fileMenu.setEnabled(toggle);
        helpMenu.setEnabled(toggle);
    }

    public void windowClosing(WindowEvent e)  
    {
        if (gui.quitConfirmed(this, null) == true)
        {
            System.exit(0);
        }
    }

    public void windowActivated(WindowEvent e)  {}
    public void windowClosed(WindowEvent e)  {}
    public void windowDeactivated(WindowEvent e)  {}
    public void windowDeiconified(WindowEvent e)  {}
    public void windowIconified(WindowEvent e)  {}
    public void windowOpened(WindowEvent e) {}

    /**
     * @param args Start up the application, display the main window and the
     *            login dialog.
     */
    public static void main(String[] args)
    {  
        ImportConfig config = new ImportConfig(args.length > 0 ? new File(args[0]) : null);
        config.loadAll();
        USE_QUAQUA = config.getUseQuaqua();
        
        String laf = UIManager.getSystemLookAndFeelClassName() ;

        //laf = "ch.randelshofer.quaqua.QuaquaLookAndFeel";
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        //laf = "javax.swing.plaf.metal.MetalLookAndFeel";

        if (laf.equals("apple.laf.AquaLookAndFeel") && USE_QUAQUA)
        {
            System.setProperty("Quaqua.design", "panther");
            
            try {
                UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
           } catch (Exception e) { System.err.println(laf + " not supported.");}
        } else {
            try {
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) 
           { System.err.println(laf + " not supported."); }
        }
        
        new GuiImporter(config);
    }

    public static Point getSplashLocation()
    {
        return null;
        //return splashLocation;
    }

    public void update(IObservable importLibrary, ImportEvent event)
    {
        if (event instanceof ImportEvent.LOADING_IMAGE)
        {
            ImportEvent.LOADING_IMAGE ev = (ImportEvent.LOADING_IMAGE) event;
            
            statusBar.setProgress(true, -1, "Loading file " + ev.numDone + " of " + ev.total);
            appendToOutput("> [" + ev.index + "] Loading image \"" + ev.shortName + "\"...\n");
            statusBar.setStatusIcon("gfx/import_icon_16.png", "Prepping file \"" + 
                    ev.shortName + "\" (file " + ev.numDone + " of " + ev.total + " imports)");
        }
        if (event instanceof ImportEvent.LOADED_IMAGE)
        {
            ImportEvent.LOADED_IMAGE ev = (ImportEvent.LOADED_IMAGE) event;
            
            statusBar.setProgress(true, -1, "Analyzing file " + ev.numDone + " of " + ev.total);
            appendToOutput(" Succesfully loaded.\n");
            appendToOutput("> [" + ev.index + "] Importing metadata for " + "image \"" + ev.shortName + "\"... ");
            statusBar.setStatusIcon("gfx/import_icon_16.png", "Analyzing the metadata for file \"" + 
                    ev.shortName + "\" (file " + ev.numDone + " of " + ev.total + " imports)");            
        }
        
        if (event instanceof ImportEvent.DATASET_STORED)
        {
            ImportEvent.DATASET_STORED ev = (ImportEvent.DATASET_STORED) event;
            
            int num = ev.index;
            int tot = ev.series;
            int pro = num - 1;
            appendToOutputLn("Successfully stored to "+ev.target.getClass().getSimpleName()+" \"" + 
                    ev.filename + "\" with id \"" + ev.target.getId().getValue() + "\".");
            appendToOutputLn("> [" + ev.series + "] Importing pixel data for " + "image \"" + ev.filename + "\"... ");
            statusBar.setProgress(true, 0, "Importing file " + num + " of " + tot);
            statusBar.setProgressValue(pro);
            statusBar.setStatusIcon("gfx/import_icon_16.png", "Importing the plane data for file \"" +
                    ev.filename + "\" (file " + num + " of " + tot + " imports)");
            appendToOutput("> Importing plane: ");
        }
        
        if (event instanceof ImportEvent.DATA_STORED)
        {
            ImportEvent.DATA_STORED ev = (ImportEvent.DATA_STORED) event;
            
            appendToOutputLn("> Successfully stored with pixels id \"" + ev.pixId + "\".");
            appendToOutputLn("> [" + ev.filename + "] Image imported successfully!");
        }
        
        if (event instanceof EXCEPTION_EVENT)
        {
            FILE_EXCEPTION ev = (FILE_EXCEPTION) event;
            if (IOException.class.isAssignableFrom(ev.exception.getClass())) {
            
                final JOptionPane optionPane = new JOptionPane( 
                        "The importer cannot retrieve one of your images in a timely manner.\n" +
                        "The file in question is:\n'" + ev.filename + "'\n\n" +
                        "There are a number of reasons you may see this error:\n" +
                        " - The file has been deleted.\n" +
                        " - There was a networking error retrieving a remotely saved file.\n" +
                        " - An archived file has not been fully retrieved from backup.\n\n" +
                        "The importer will now try to continue with the remainer of your imports.\n",
                        JOptionPane.ERROR_MESSAGE);
            
                final JDialog dialog = new JDialog(this, "IO Error");
                dialog.setAlwaysOnTop(true);
                dialog.setContentPane(optionPane);
                dialog.pack();
                dialog.setVisible(true);
                
            }
        }
        
        
        if (event instanceof ImportEvent.ERRORS_PENDING)
        {
            tPane.setIconAt(4, gui.getImageIcon(ERROR_ICON_ANIM));
        }
        
        if (event instanceof ImportEvent.ERRORS_COMPLETE)
        {
            tPane.setIconAt(4, gui.getImageIcon(ERROR_ICON));
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();
        if (ScreenLogin.LOGIN_PROPERTY.equals(name)) {
            LoginCredentials lc = (LoginCredentials) evt.getNewValue();
            if (lc != null) login(lc);
        } else if (ScreenLogin.QUIT_PROPERTY.equals(name) || name.equals("quitpplication")) {
            if (gui.quitConfirmed(this, null) == true)
            {
                System.exit(0);
            }
        } else if (ScreenLogin.TO_FRONT_PROPERTY.equals(name) || 
                ScreenLogo.MOVE_FRONT_PROPERTY.equals(name)) {
            //updateView();
        } else if (name.equals("aboutApplication"))
        {
            // HACK - JOptionPane prevents shutdown on dispose
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            About.show(this, config, useSplashScreenAbout);
        }
        
        
    }

    public void login(LoginCredentials lc) {}
    public void windowStateChanged(WindowEvent arg0) {}
    public void windowGainedFocus(WindowEvent arg0) {}
    public void windowLostFocus(WindowEvent arg0) {}
}