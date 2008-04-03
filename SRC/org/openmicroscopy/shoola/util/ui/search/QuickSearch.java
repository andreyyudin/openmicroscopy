/*
 * org.openmicroscopy.shoola.util.ui.QuickSearch 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2007 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.util.ui.search;


//Java imports
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

//Third-party libraries
import layout.TableLayout;

//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.IconManager;
import org.openmicroscopy.shoola.util.ui.UIUtilities;

/** 
 * Basic panel with text field for searching.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class QuickSearch
	extends JPanel
	implements ActionListener, DocumentListener, PropertyChangeListener
{

	/** Indicates to search for tags. */
	public static final int 	TAGS = 0;
	
	/** Indicates to search for textual annotations. */
	public static final int 	COMMENTS = 1;
	
	/** Indicates to search for textual annotations. */
	public static final int 	FULL_TEXT = 2;
	
	/** Indicates to search for objects rated one or higher. */
	public static final int 	RATED_ONE_OR_BETTER = 3;
	
	/** Indicates to search for objects rated two or higher. */
	public static final int 	RATED_TWO_OR_BETTER = 4;
	
	/** Indicates to search for objects rated three or higher. */
	public static final int 	RATED_THREE_OR_BETTER = 5;
	
	/** Indicates to search for objects rated four or higher. */
	public static final int 	RATED_FOUR_OR_BETTER = 6;
	
	/** Indicates to search for objects rated five. */
	public static final int 	RATED_FIVE = 7;
	
	/** Indicates to search for objects unrated. */
	public static final int 	UNRATED = 8;
	
	/** Indicates to reset all nodes. */
	public static final int 	SHOW_ALL = 9;
	
	/** Indicates to search for th untagged nodes. */
	public static final int 	UNTAGGED = 10;
	
	/** Indicates to search for th uncommented nodes.  */
	public static final int 	UNCOMMENTED = 11;
	
	/** Bound property indicating to search for given terms. */
	public static final String	QUICK_SEARCH_PROPERTY = "quickSearch";
	
	/** Removes the text from the text field. */
	private static final int 	CLEAR = 0;
	
	/** The selected node. */
	private SearchObject		selectedNode;
	
	/** Area where to enter the tags to search. */
	private JTextField			searchArea;
	
	/** Button to look nice. Does nothing for now. */
	private JButton				searchButton;
	
	/** Button to clear the text. */
	private JButton				clearButton;
	
	/** Button to clear the text. */
	private JButton				menuButton;
	
	/** The Layout manager used to lay out the search area. */
	private TableLayout			layoutManager;
	
	/** UI Component hosting the various items for the search. */
	private JPanel				searchPanel;
	
	/** The popup menu. */
	private SearchContextMenu	menu;
	
	/** The possible options. */
	private List<SearchObject> 	nodes;

	/** The possible options. */
	private List<SearchObject> 	ratedNodes;
	
	/** Label if any. */
	private String				label;

	/** 
	 * Flag indicating that only one context can be selected at a time
	 * if set to <code>true</code>, more than one context if set 
	 * to <code>false</code>.
	 */
	private boolean				singleSelection;
	
	/** Shows the menu. */
	private void showMenu()
	{
		Rectangle r = searchPanel.getBounds();
        if (menu == null) {
        	if (selectedNode != null)
        		menu = new SearchContextMenu(nodes, ratedNodes, r.width, 
        									selectedNode, singleSelection);
        	else 
        		menu = new SearchContextMenu(nodes, ratedNodes, r.width, 
        									singleSelection);
        	menu.addPropertyChangeListener(
        			SearchContextMenu.SEARCH_CONTEXT_PROPERTY, this);
        }
        menu.show(searchPanel, 0, r.height);
	}
	
	/** Initializes the components composing the display. */
	private void initComponents()
	{
		IconManager icons = IconManager.getInstance();
		clearButton = new JButton(icons.getIcon(IconManager.CLEAR_DISABLED));
		//clearButton.setEnabled(false);
		UIUtilities.setTextAreaDefault(clearButton);
		clearButton.setBorder(null);
		clearButton.addActionListener(this);
		clearButton.setActionCommand(""+CLEAR);
		
		searchArea = new JTextField(15);
        UIUtilities.setTextAreaDefault(searchArea);
        searchArea.setBorder(null);
        
        searchArea.getDocument().addDocumentListener(this);
        searchArea.addKeyListener(new KeyAdapter() {

            /** Finds the phrase. */
            public void keyPressed(KeyEvent e)
            {
            	Object source = e.getSource();
            	if (source != searchArea) return;
            	switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						handleKeyEnter();
						break;
					
				}
            }
        });
	}
	
	/** 
	 * Initializes the components composing the display.
	 *  
	 * @param icon The search icon.
	 */
	private void initComponents(Icon icon)
	{
		IconManager icons = IconManager.getInstance();
		if (icon == null)
			icon = icons.getIcon(IconManager.SEARCH);
		searchButton = new JButton(icon);
		UIUtilities.setTextAreaDefault(searchButton);
		searchButton.setBorder(null);
		initComponents();
	}
	
	/**
	 * Initializes the menu and related components. 
	 * 
	 * @param nodes 		The nodes to display.
	 * @param ratedNodes 	The rated nodes if any.
	 */
	private void initSearchComponents(List<SearchObject> nodes, 
									List<SearchObject> ratedNodes)
	{
		if (ratedNodes == null) ratedNodes = new ArrayList<SearchObject>();
		this.nodes = nodes;
		this.ratedNodes = ratedNodes;
		IconManager icons = IconManager.getInstance();
		//check when to create it
		menuButton = new JButton(icons.getIcon(IconManager.FILTER_MENU));
		menuButton.setToolTipText("Display the search options");
		UIUtilities.setTextAreaDefault(menuButton);
		menuButton.setBorder(null);
		menuButton.addMouseListener(new MouseAdapter() {
		
			/** 
			 * Displays a menu with the available context.
			 * @see MouseAdapter#mousePressed(MouseEvent)
			 */
			public void mousePressed(MouseEvent e) { showMenu(); }
		
		});
		selectedNode = nodes.get(0);
		searchButton = new JButton(selectedNode.getIcon());
		//searchButton.setEnabled(false);
		UIUtilities.setTextAreaDefault(searchButton);
		searchButton.setBorder(null);
		initComponents();
		searchArea.setToolTipText(selectedNode.getDescription());
	}
	
	/** 
	 * Builds and lays out the UI. 
	 *
	 * @param label The text to display in front of the text field.
	 */
	private void buildGUI(String label)
	{
		double w = 0;
		if (menuButton != null) w = TableLayout.PREFERRED;
		double[][] pl = {{TableLayout.PREFERRED, w, TableLayout.FILL, 0}, 
				{TableLayout.PREFERRED} }; //rows
		layoutManager = new TableLayout(pl);
		
		searchPanel = new JPanel();
		UIUtilities.setTextAreaDefault(searchPanel);
		searchPanel.setLayout(layoutManager);
		searchPanel.setBorder(
					BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		//searchPanel.add(searchButton, "0, 0, f, c");
		if (menuButton != null) 
			searchPanel.add(menuButton, "1, 0, f, c");
		searchPanel.add(searchArea, "2, 0, f, c");
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setOpaque(true);
		if (label != null && label.trim().length() > 0) 
			add(UIUtilities.setTextFont(label));
		add(searchPanel);
	}
	
	/** Fires a property change to search for some tags. */
	private void handleKeyEnter()
	{
		List<String> l = SearchUtil.splitTerms(searchArea.getText(), 
											SearchUtil.COMMA_SEPARATOR);
		
		if (selectedNode == null) selectedNode = new SearchObject();
		selectedNode.setResult(l);
		firePropertyChange(QUICK_SEARCH_PROPERTY, null, selectedNode);
	}
	
	/** Removes the text from the display. */
	private void clear()
	{
		searchArea.getDocument().removeDocumentListener(this);
		searchArea.setText("");
		searchArea.getDocument().addDocumentListener(this);
		layoutManager.setColumn(3, 0);
		//searchPanel.remove(clearButton);
		searchPanel.validate();
		searchPanel.repaint();
	}
	
	/** Displays the context of the search. */
	private void setSearchContext()
	{
		String text = null;
		if (selectedNode == null) return;
		switch (selectedNode.getIndex()) {
			case RATED_ONE_OR_BETTER:
			case RATED_TWO_OR_BETTER:
			case RATED_THREE_OR_BETTER:
			case RATED_FOUR_OR_BETTER:
			case RATED_FIVE:
			case SHOW_ALL:
			case UNRATED:
			case UNTAGGED:
			case UNCOMMENTED:
				text = selectedNode.getDescription();
		}
		if (text == null) return;
		searchArea.getDocument().removeDocumentListener(this);
		searchArea.setText(text);
		searchArea.getDocument().addDocumentListener(this);
	}
	
	/** Creates a new instance. */
	public QuickSearch()
	{
		initComponents(null);
		buildGUI(null);
	}
	
	/** 
	 * Creates a new instance.
	 * 
	 * @param icon The search icon.
	 */
	public QuickSearch(Icon icon)
	{
		this(null, icon);
	}

	/** 
	 * Creates a new instance.
	 * 
	 * @param label The text displayed in front of the panel.
	 */
	public QuickSearch(String label)
	{
		this.label = label;
		initComponents(null);
		buildGUI(label);
	}
	
	/** 
	 * Creates a new instance.
	 * 
	 * @param label The text displayed in front of the panel.
	 * @param icon  The search icon.
	 */
	public QuickSearch(String label, Icon icon)
	{
		this.label = label;
		initComponents(icon);
		buildGUI(label);
	}
	
	/** 
	 * Creates a new instance.
	 * 
	 * @param nodes			The nodes describing the context of the search.
	 * @param ratedNodes 	The rated nodes if any.
	 */
	public QuickSearch(List<SearchObject> nodes, List<SearchObject> ratedNodes)
	{
		this(null, nodes, ratedNodes);
	}
	
	/** 
	 * Creates a new instance.
	 * 
	 * @param label			The text displayed in front of the panel.
	 * @param nodes			The nodes describing the context of the search.
	 * @param ratedNodes 	The rated nodes if any.
	 */
	public QuickSearch(String label, List<SearchObject> nodes, 
						List<SearchObject> ratedNodes)
	{
		this.label = label;
		if (nodes == null || nodes.size() == 0)
			initComponents(null);
		else initSearchComponents(nodes, ratedNodes);
		buildGUI(label);
	}
	
	/** 
	 * Sets to <code>true</code> if only one context can be selected at a time
	 * to <code>false</code> otherwise.
	 * 
	 * @param singleSelection The value to set.
	 */
	public void setSingleSelection(boolean singleSelection)
	{
		this.singleSelection = singleSelection;
	}
	
	/** Adds the default searching context. */
	public void setDefaultSearchContext()
	{
		List<SearchObject> nodes = new ArrayList<SearchObject>();
    	SearchObject node = new SearchObject(FULL_TEXT, null, 
    								"Name/Description search");
    	nodes.add(node);
    	node = new SearchObject(TAGS, null, "Tags search");
    	nodes.add(node);
    	node = new SearchObject(COMMENTS, null, "Comments search");
    	nodes.add(node);
    	node = new SearchObject(UNTAGGED, null, "Untagged");
    	nodes.add(node);
    	node = new SearchObject(UNCOMMENTED, null, "Uncommented");
    	nodes.add(node);
    	
    	List<SearchObject> ratedNodes = new ArrayList<SearchObject>();
    	node = new SearchObject(RATED_ONE_OR_BETTER, null, "* or better");
    	ratedNodes.add(node);
    	node = new SearchObject(RATED_TWO_OR_BETTER, null, "** or better");
    	ratedNodes.add(node);
    	node = new SearchObject(RATED_THREE_OR_BETTER, null, "*** or better");
    	ratedNodes.add(node);
    	node = new SearchObject(RATED_FOUR_OR_BETTER, null, "**** or better");
    	ratedNodes.add(node);
    	node = new SearchObject(RATED_FIVE, null, "*****");
    	ratedNodes.add(node);
    	node = new SearchObject(SHOW_ALL, null, "Show All");
    	ratedNodes.add(node);
    	node = new SearchObject(UNRATED, null, "Unrated");
    	ratedNodes.add(node);
    	initSearchComponents(nodes, ratedNodes);
    	removeAll();
    	buildGUI(label);
	}
	
	/**
	 * Returns the selected node.
	 * 
	 * @return See above.
	 */
	public SearchObject getSelectedNode() { return selectedNode; }

	/**
	 * Returns the search area.
	 * 
	 * @return See above.
	 */
	public JComponent getSelectionArea() { return searchPanel; }
	
	/** Sets the focus on the {@link #searchArea}. */
	public void setFocusOnArea() { searchArea.requestFocus(); }
	
	/**
	 * Returns the text of the {@link #searchArea}.
	 * 
	 * @return See above.
	 */
	public String getSearchValue() { return searchArea.getText(); }
	
	/**
	 * Sets the value of the {@link #searchArea}.
	 * 
	 * @param text The value to set.
	 */
	public void setSearchValue(String text)
	{
		if (text == null) return;
		String result = "";
    	List<String> l = SearchUtil.splitTerms(getSearchValue(), 
				SearchUtil.COMMA_SEPARATOR);
    	if (l != null) {
    		Iterator<String> i = l.iterator();
    		boolean exist = false;
    		String value;
    		while (i.hasNext()) {
    			value = i.next();
				if (value != null && value.equals(text))
					exist = true;
			}
    		if (exist) {
    			i = l.iterator();
    			int index = 0;
    			int n = l.size()-1;
    			while (i.hasNext()) {
        			value = i.next();
        			result += value;
    				if (index != n) 
    					result += SearchUtil.COMMA_SEPARATOR+" ";
    				index++;
    			}
    		} else {
    			i = l.iterator();
    			while (i.hasNext()) {
        			value = i.next();
        			result += value;
    				result += SearchUtil.COMMA_SEPARATOR+" ";
    			}
    			result += text;
    		}
    	} else result = text;
    	searchArea.getDocument().removeDocumentListener(this);
    	searchArea.setText(result);
    	searchArea.getDocument().addDocumentListener(this);
	}
	
	/** 
	 * Class extended this class should override that method for 
	 * code completion.
	 */
	protected void handleTextInsert() {}
	
	/** 
     * Shows or hides the {@link #clearButton} when some text is entered.
     * @see DocumentListener#insertUpdate(DocumentEvent)
     */
	public void insertUpdate(DocumentEvent e)
	{
		handleTextInsert();
		layoutManager.setColumn(3, TableLayout.PREFERRED);
		//searchPanel.add(clearButton, "3, 0, f, c");
		searchPanel.validate();
		searchPanel.repaint();
	}

	/** 
     * Shows or hides the {@link #clearButton} when some text is entered.
     * @see DocumentListener#removeUpdate(DocumentEvent)
     */
	public void removeUpdate(DocumentEvent e)
	{
		if (e.getDocument().getLength() == 0) clear();
	}
	
	/** Subclasses should override this method to handle the search. */
	public void search() {}
	
	/**
	 * Clears the text from the display.
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		int index = Integer.parseInt(e.getActionCommand());
		switch (index) {
			case CLEAR:
				clear();
				break;
		}
	}
	
	/**
	 * Reacts to the property fired by the <code>SearchContextMenu</code>
	 * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		String name = evt.getPropertyName();
		if (SearchContextMenu.SEARCH_CONTEXT_PROPERTY.equals(name)) {
			SearchObject node = (SearchObject) evt.getNewValue();
			if (node != null) selectedNode = node;
			searchButton.setIcon(node.getIcon());
			UIUtilities.setTextAreaDefault(searchButton);
			searchButton.setBorder(null);
			searchArea.setToolTipText(node.getDescription());
			setSearchContext();
			handleKeyEnter();
		} else if (QUICK_SEARCH_PROPERTY.equals(name)) search();
	}
	
	/** 
     * Required by I/F but no-op implementation in our case. 
     * @see DocumentListener#changedUpdate(DocumentEvent)
     */
	public void changedUpdate(DocumentEvent e) {}

}
