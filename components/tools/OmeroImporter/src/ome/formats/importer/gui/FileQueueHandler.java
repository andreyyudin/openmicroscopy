/*
 * ome.formats.importer.gui.FileQueueHandler
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import omero.model.Dataset;
import omero.model.IObject;
import omero.model.Screen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class FileQueueHandler 
    extends JPanel 
    implements ActionListener, PropertyChangeListener, IObserver
{
    /** Logger for this class */
    private static final Log log = LogFactory.getLog(FileQueueHandler.class);

    public final static String ADD = "add";
    public final static String REMOVE = "remove";
    public final static String CLEARDONE = "clear_done";
    public final static String CLEARFAILED = "clear_failed";
    public final static String IMPORT = "import";
    public final static String REFRESH = "refresh";
    
	private final ImportConfig config;
    private final OMEROWrapper reader;
    private final GuiImporter viewer;
    private final GuiCommonElements gui;
    
    private final FileQueueChooser fileChooser;
    private final FileQueueTable qTable;
    private final HistoryTable historyTable;
    
    /**
     * @param viewer
     */
    FileQueueHandler(GuiImporter viewer, ImportConfig config)
    {
        this.config = config;
        this.viewer = viewer;
        this.reader = new OMEROWrapper(config);
        this.gui = new GuiCommonElements(config);
        this.historyTable = viewer.historyTable;

        //reader.setChannelStatCalculationStatus(true);
        
        setLayout(new BorderLayout());
        fileChooser = new FileQueueChooser(config, reader);
        fileChooser.addActionListener(this);
        fileChooser.addPropertyChangeListener(this);
        
        //fc.setAccessory(new FindAccessory(fc));
        
        qTable = new FileQueueTable(config);
        qTable.addPropertyChangeListener(this);
        
        // Functionality to allows the reimport button to work
        if (historyTable != null)
        {
            historyTable.addObserver(this);
            addPropertyChangeListener(historyTable);
        }
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                fileChooser, qTable);
        
        splitPane.setResizeWeight(0.1);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    protected OMEROMetadataStoreClient store() {
        return viewer.loginHandler.getMetadataStore();
    }

    public void actionPerformed(ActionEvent e)
    {
        final String action = e.getActionCommand();
        final File[] files = fileChooser.getSelectedFiles();
        
        //If the directory changed, don't show an image.
        if (action.equals(JFileChooser.APPROVE_SELECTION)) {
            String msg = "DISABLED BY JOSH";
            log.error(msg);
            if (true) {
                throw new RuntimeException(msg);
            }
            
            if (false) { // DELETE THIS SECTION. COPY OF ANOTHER IN THIS FILE.
            
            File file = fileChooser.getSelectedFile();
            
            if (store() != null && files != null && reader.isSPWReader(files[0].getAbsolutePath()))
            {
                SPWDialog dialog =
                    new SPWDialog(gui, viewer, "Screen Import", true, store());
                if (dialog.cancelled == true || dialog.screen == null) 
                    return;                    
                for (File f : files)
                {             
                    /*
                    addFileToQueue(f, dialog.screen, 
                            dialog.screen.getName().getValue(),
                            null, 
                            false, 
                            0,
                            dialog.archiveImage.isSelected(),
                            null, null, reader.getFormat(), reader.getUsedFiles(), true);
                     */
                }
                
                qTable.centerOnRow(qTable.queue.getRowCount()-1);
                qTable.importBtn.requestFocus();
            }
            else if (store() != null)
            {
                ImportDialog dialog = 
                    new ImportDialog(gui, viewer, "Import", true, store());
                if (dialog.cancelled == true || dialog.dataset == null) 
                    return;
                
                Double[] pixelSizes = new Double[] {dialog.pixelSizeX, dialog.pixelSizeY, dialog.pixelSizeZ};
                
                Boolean useFullPath = dialog.useFullPath;
                
                if (dialog.fileCheckBox.isEnabled() == true)
                    useFullPath = null;                    
                /*
                addFileToQueue(file, dialog.dataset,
                        dialog.dataset.getName().getValue(), dialog.project.getName().getValue(), 
                        useFullPath, dialog.numOfDirectories, 
                        dialog.archiveImage.isSelected(), dialog.project.getId().getValue(),
                        pixelSizes, null, null, false);
                */
                qTable.importBtn.requestFocus();
                
            } else { 
                JOptionPane.showMessageDialog(viewer, 
                        "Due to an error the application is unable to \n" +
                        "retrieve an OMEROMetadataStore and cannot continue." +
                        "The most likely cause for this error is that you" +
                "are not logged in. Please try to login again.");
            }
            }
        }
    }

    private void mustSelectFile()
    {
        JOptionPane.showMessageDialog(viewer, 
                "You must select at least one importable file to\n" +
                "add to the import queue. Choose an image in the\n" +
                "left-hand panel first before continuing.");
    }
    
    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();
        if (prop.equals(ADD))
        {
            
            final File[] _files = fileChooser.getSelectedFiles();                    

            if (_files == null)
            {
                mustSelectFile();
                return;
            }

            String[] paths = new String[_files.length];
            for (int i = 0; i < paths.length; i++) {
                paths[i] = _files[i].getAbsolutePath();
            }

            final ImportCandidates candidates = new ImportCandidates(reader, paths);
            final List<ImportContainer> containers = candidates.getContainers();
            
            Boolean spw = spwOrNull(containers);
            if (spw == null) {
                return; // Invalid containers.
            }
            
            if (store() != null && spw.booleanValue())
            {
                SPWDialog dialog =
                    new SPWDialog(gui, viewer, "Screen Import", true, store());
                if (dialog.cancelled == true || dialog.screen == null) 
                    return;                    
                for (ImportContainer ic : containers)
                {             
                    ic.setTarget(dialog.screen);
                    String title = dialog.screen.getName().getValue(); 
                    addFileToQueue(ic, title, false, 0);
                }
                
                qTable.centerOnRow(qTable.queue.getRowCount()-1);
            }
            else if (store() != null)
            {
                ImportDialog dialog = 
                    new ImportDialog(gui, viewer, "Image Import", true, store());
                if (dialog.cancelled == true || dialog.dataset == null) 
                    return;  

                
                Double[] pixelSizes = new Double[] {dialog.pixelSizeX, dialog.pixelSizeY, dialog.pixelSizeZ};
                Boolean useFullPath = dialog.useFullPath;
                if (dialog.fileCheckBox.isSelected() == false)
                    useFullPath = null; //use the default bio-formats naming
                    
                
                for (ImportContainer ic : containers)
                {
                    ic.setTarget(dialog.dataset);
                    ic.setUserPixels(pixelSizes);
                    ic.setArchive(dialog.archiveImage.isSelected());
                    String title =
                    dialog.project.getName().getValue() + " / " +
                    dialog.dataset.getName().getValue();
                    
                    addFileToQueue(ic, title, useFullPath, dialog.numOfDirectories);
                }
                
                qTable.centerOnRow(qTable.queue.getRowCount()-1);
                
            } else {
                JOptionPane.showMessageDialog(viewer, 
                        "Due to an error the application is unable to \n" +
                        "retrieve an OMEROMetadataStore and cannot continue." +
                        "The most likely cause for this error is that you" +
                        "are not logged in. Please try to login again.");
            }
        }
        
        else if (prop.equals(REMOVE))
        {
                int[] rows = qTable.queue.getSelectedRows();   

                if (rows.length == 0)
                {
                    JOptionPane.showMessageDialog(viewer, 
                            "You must select at least one file in the queue to\n" +
                            "remove. Choose an image in the right-hand panel \n" +
                    "first before removing.");
                    return;
                }

                while (rows.length > 0)
                {
                    if (qTable.queue.getValueAt(rows[0], 2) == "added"
                        || qTable.queue.getValueAt(rows[0], 2) == "pending")
                    {
                        removeFileFromQueue(rows[0]);
                        rows = qTable.queue.getSelectedRows();                    
                    }
                }                
        }
        
        else if (prop.equals(CLEARDONE))
        {
                int numRows = qTable.queue.getRowCount();

                for (int i = (numRows - 1); i >= 0; i--)
                {
                    if (qTable.queue.getValueAt(i, 2) == "done")
                    {
                        removeFileFromQueue(i);                    
                    }
                }
                qTable.clearDoneBtn.setEnabled(false);
        }
        
        else if (prop.equals(CLEARFAILED))
        {
                int numRows = qTable.queue.getRowCount();

                for (int i = (numRows - 1); i >= 0; i--)
                {
                    if (qTable.queue.getValueAt(i, 2) == "failed")
                    {
                        removeFileFromQueue(i);                    
                    }
                }  
                qTable.clearFailedBtn.setEnabled(false);
        }
       
        else if (prop.equals(IMPORT))
        {
            if (viewer.loggedIn == false)
            {
                JOptionPane.showMessageDialog(viewer, 
                        "You must be logged in before you can import.");
                return;
            }
            
            qTable.clearDoneBtn.setEnabled(false);
            qTable.clearFailedBtn.setEnabled(false);
            try {
                if (qTable.importing == false)
                {
                    ImportContainer[] candidates = qTable.getFilesAndObjectTypes();

                    if (candidates != null)
                    {
                        ImportLibrary library = new ImportLibrary(store(), reader);
                        if (store() != null) {
                            new ImportHandler(viewer, qTable, config, library, candidates);
                        }
                    }
                    qTable.importing = true;
                    qTable.queue.setRowSelectionAllowed(false);
                    qTable.removeBtn.setEnabled(false);
                } else {
                    qTable.importBtn.setText("Wait...");
                    qTable.importBtn.setEnabled(false);
                    viewer.statusBar.setStatusIcon("gfx/import_cancelling_16.png",
                    "Cancelling import... please wait.");
                    //JOptionPane.showMessageDialog(viewer, 
                    //        "You import will be cancelled after the " +
                    //        "current file has finished importing.");
                    if (cancelImportDialog(viewer) == true)
                    {
                        qTable.cancel = true;
                        qTable.abort = true;
                        qTable.importing = false;
                        System.exit(0);
                    } else {
                        qTable.cancel = true;
                        qTable.importing = false;
                    }
                }
            } catch (Exception ex) {
            	log.error("Generic error while updating GUI for import.", ex);
                return;  
            }
        }
        
        else if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY))
        {
            config.setSavedDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
        }       
        
        else if (prop.equals(REFRESH))
        {
            fileChooser.setVisible(false);
            fileChooser.rescanCurrentDirectory();
            fileChooser.setVisible(true);
        }
    }

    /**
     * Checks whether all the candidate imports in the list are either SPW or
     * not SPW. If there is a mismatch, then a warning is shown and null returned,
     * otherwise a Boolean of whether or not this is a SPW import will be returned.
     */
    private Boolean spwOrNull(final List<ImportContainer> containers) {
        Boolean isSPW = null;
        for (ImportContainer importContainer : containers) {
            if (isSPW != null && importContainer.isSPW != isSPW.booleanValue()) {
                JOptionPane.showMessageDialog(viewer, 
                        "You have chosen some Screen-based images and some \n "+
                        "non-screen-based images. Please import only one type at a time.");
                return null;
            }
            isSPW = importContainer.isSPW;
        }
        return isSPW;
    }
    
    
    private boolean cancelImportDialog(Component frame) {
        String s1 = "OK";
        String s2 = "Force Quit Now";
        Object[] options = {s1, s2};
        int n = JOptionPane.showOptionDialog(frame,
                "Click 'OK' to cancel after the current file has\n" +
                "finished importing, or click 'Force Quit Now' to\n" +
                "force the importer to quit importing immediately.\n\n" +
                "You should only force quit the importer if there\n" +
                "has been an import problem, as this leaves partial\n" +
                "files in your server dataset.\n\n",
                "Cancel Import",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                s1);
        if (n == JOptionPane.NO_OPTION) {
            return true;
        } else {
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addFileToQueue(ImportContainer container, String pdsString, Boolean useFullPath, int numOfDirectories) {
        Vector row = new Vector();
        String imageName;
        
        if (useFullPath != null) {
            imageName = getImageName(container.file, useFullPath, numOfDirectories);
        } else {
            imageName = container.file.getAbsolutePath();
        }

        row.add(imageName);
        row.add(pdsString);
        row.add("added");
        row.add(container);
        qTable.table.addRow(row);
        if (qTable.table.getRowCount() == 1)
            qTable.importBtn.setEnabled(true);
    }
    
    /**
     * Return a stipped down string containing the file name and X number of directories above it.
     * Used for display purposes.
     * @param file
     * @param useFullPath
     * @param numOfDirectories
     * @return
     */
    private String getImageName(File file, Boolean useFullPath, int numOfDirectories)
    {
       // standardize the format of files from window '\' to unix '/' 
       String path = file.getAbsolutePath().replace( '\\', '/' );
        
       if (useFullPath == true) return path;
       else if (numOfDirectories == 0) return file.getName();      
       else 
       {
           String[] directories = splitDirectories(path);
           if (numOfDirectories > directories.length - 1) 
               numOfDirectories = directories.length - 1;
           
           int start = directories.length - numOfDirectories - 1;
           
           String fileName = "";
               
           for (int i = start; i < directories.length - 1; i++)
           {
               //viewer.appendToDebugLn(directories[i]);
               if (directories[i].length() != 0)
               {
                   if (i == start)
                   {
                       fileName = directories[i];
                   } else
                   {
                       fileName = fileName + "/" + directories[i];                       
                   }
               }
           }

           fileName = fileName + "/" + file.getName();  
           
           return fileName;
       }
    }

    // Split the directories by file seperator character ("/" or "\")
    private String[] splitDirectories(String path)
    {
        //viewer.appendToDebugLn(path);
        String[] fields = path.split("/");
        @SuppressWarnings("unused")
        Integer length = fields.length;
        //viewer.appendToDebugLn(length.toString());
       
        
        return fields;
    }
    
    private void removeFileFromQueue(int row)
    {
        qTable.table.removeRow(row);
        //qTable.table.fireTableRowsDeleted(row, row);
        if (qTable.table.getRowCount() == 0)
            qTable.importBtn.setEnabled(false);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String laf = UIManager.getSystemLookAndFeelClassName() ;
        //laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        //laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        //laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        //laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        
        if (laf.equals("apple.laf.AquaLookAndFeel"))
        {
            System.setProperty("Quaqua.design", "panther");
            
            try {
                UIManager.setLookAndFeel(
                    "ch.randelshofer.quaqua.QuaquaLookAndFeel"
                );
           } catch (Exception e) { System.err.println(laf + " not supported.");}
        } else {
            try {
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) 
            { System.err.println(laf + " not supported."); }
        }
        
        FileQueueHandler fqh = new FileQueueHandler(null, null); 
        JFrame f = new JFrame();   
        f.getContentPane().add(fqh);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
    }

    @SuppressWarnings("unchecked")
    public void update(IObservable observable, ImportEvent event)
    {
        final OMEROMetadataStoreClient store = viewer.loginHandler.getMetadataStore();  

        if (event instanceof ImportEvent.REIMPORT)
        {
            
            String objectName = "", projectName = "", fileName = "";
            Long objectID = 0L, projectID = 0L;
            File file = null;
            Integer finalCount = 0;
            IObject object;
            
            int count = 0;
            
            if (historyTable != null)
                count = historyTable.table.getRowCount();

            for (int r = 0; r < count; r++)
            {
                Vector row = new Vector();
                
                objectID = (Long) historyTable.table.getValueAt(r, 5);
                projectID = (Long) historyTable.table.getValueAt(r, 6);
                
                fileName = (String) historyTable.table.getValueAt(r, 0);
                file = new File((String) historyTable.table.getValueAt(r, 4));
                
                if (projectID == null || projectID == 0)
                {
                    object = null;

                    try {
                        object = store.getTarget(Screen.class, objectID);
                        objectName = ((Screen)object).getName().getValue();
                    } catch (Exception e)
                    {
                        log.warn("Failed to retrieve screen: " + objectID, e);
                        continue;
                    }                    
                }
                else
                {
                    object = null;
                    try {
                    	object = store.getTarget(Dataset.class, objectID);
                        objectName = ((Dataset)object).getName().getValue();
                    } catch (Exception e)
                    {
                    	log.warn("Failed to retrieve dataset: " + objectID, e);
                        continue;
                    } 
                    
                    try {
                        projectName = store.getProject(projectID).getName().getValue();
                    } catch (Exception e)
                    {
                        log.warn("Failed to retrieve project: " + projectID, e);
                        continue;
                    }
                }
                
                finalCount = finalCount + 1;
                
                Double[] pixelSizes = new Double[] {1.0, 1.0, 1.0};
                
                row.add(fileName);
                if (projectID == null || projectID == 0)
                {
                    row.add(objectName); 
                }
                else
                {
                    row.add(projectName + "/" + objectName);   
                }
                // WHY ISN'T THIS CODE USING addFiletoQueue?!!?
                row.add("added");
                row.add(object);
                row.add(file);
                row.add(false);
                row.add(projectID);
                row.add(pixelSizes);
                qTable.table.addRow(row);
            }
            
            if (finalCount == 0)
            {
                JOptionPane.showMessageDialog(viewer, 
                        "None of the images in this history\n" +
                        "list can be reimported.");                
            } else if (finalCount == 1)
            {
                JOptionPane.showMessageDialog(viewer, 
                        "One of the images in this history list has been\n" +
                        "re-added to the import queue for reimport.");                 
            } else if (finalCount > 1)
            {
                JOptionPane.showMessageDialog(viewer, 
                        finalCount + " images in this history list have been re-added\n" +
                        "to the import queue for reimport.");                 
            }

            
            if (qTable.table.getRowCount() >  0)
                qTable.importBtn.setEnabled(true);
        }
    }
    
    
}


/*

Vector row = new Vector();

String imageName = getImageName(file, useFullPath, numOfDirectories);
       
row.add(imageName);
row.add(project + "/" + dName);
row.add("added");
row.add(dataset);
row.add(file);
row.add(archiveImage);
row.add(projectID);
qTable.table.addRow(row);
if (qTable.table.getRowCount() == 1)
    qTable.importBtn.setEnabled(true);

*/