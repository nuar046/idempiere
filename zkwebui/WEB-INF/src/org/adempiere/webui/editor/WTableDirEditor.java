/******************************************************************************
 * Product: Posterita Ajax UI 												  *
 * Copyright (C) 2007 Posterita Ltd.  All Rights Reserved.                    *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Posterita Ltd., 3, Draper Avenue, Quatre Bornes, Mauritius                 *
 * or via info@posterita.org or http://www.posterita.org/                     *
 *****************************************************************************/

package org.adempiere.webui.editor;

import java.beans.PropertyChangeEvent;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.adempiere.webui.ValuePreference;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.event.ContextMenuEvent;
import org.adempiere.webui.event.ContextMenuListener;
import org.adempiere.webui.event.ValueChangeEvent;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;
import org.compiere.util.ValueNamePair;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;

/**
 *
 * @author  <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @date    Mar 12, 2007
 * @version $Revision: 0.10 $
 */
public class WTableDirEditor extends WEditor implements ListDataListener, 
ContextMenuListener, IZoomableEditor
{
    public final static String[] LISTENER_EVENTS = {Events.ON_SELECT};
    
    private static final CLogger logger;
    
    static
    {
        logger = CLogger.getCLogger(WTableDirEditor.class);
    }
    
    private Lookup  lookup;
    private Object oldValue;
    private WEditorPopupMenu popupMenu;
       
    public WTableDirEditor(GridField gridField)
    {
        super(new Listbox(), gridField);
        lookup = gridField.getLookup();
        init();
    }
	
	/** 
	 * Constructor for use if a grid field is unavailable
	 * 
	 * @param lookup		Store of selectable data
	 * @param label			column name (not displayed)
	 * @param description	description of component
	 * @param mandatory		whether a selection must be made
	 * @param readonly		whether or not the editor is read only
	 * @param updateable	whether the editor contents can be changed
	 */   
    public WTableDirEditor(Lookup lookup, String label, String description, boolean mandatory, boolean readonly, boolean updateable)
	{
		super(new Listbox(), label, description, mandatory, readonly, updateable);
		
		if (lookup == null)
		{
			throw new IllegalArgumentException("Lookup cannot be null");
		}
		
		this.lookup = lookup;
		super.setColumnName(lookup.getColumnName());
		init();
	}
    
    /**
     * For ease of porting swing form
     * @param columnName
     * @param mandatory
     * @param isReadOnly
     * @param isUpdateable
     * @param lookup
     */
    public WTableDirEditor(String columnName, boolean mandatory, boolean isReadOnly, boolean isUpdateable,
    		Lookup lookup)
    {
    	super(new Listbox(), columnName, null, null, mandatory, isReadOnly, isUpdateable);
    	if (lookup == null)
		{
			throw new IllegalArgumentException("Lookup cannot be null");
		}
    	this.lookup = lookup;
    	init();
    }
    
    private void init()
    {
    	getComponent().setRows(0);
        getComponent().setMultiple(false);
        getComponent().setMold("select");
        getComponent().setWidth("200px"); 
        getComponent().addPropertyChangeListener(this);

        boolean zoom= false;
        if (lookup != null)
        {
            lookup.addListDataListener(this);
            lookup.setMandatory(isMandatory());
            
            if ((lookup.getDisplayType() == DisplayType.List && Env.getContextAsInt(Env.getCtx(), "#AD_Role_ID") == 0)
            		|| lookup.getDisplayType() != DisplayType.List) 
            {
    			zoom= true;
            }
            
            //no need to refresh readonly lookup
            if (isReadWrite())
            	lookup.refresh();
            refreshList();
        }
        
        if (gridField != null) 
        {
        	popupMenu = new WEditorPopupMenu(zoom, true, true);
        	getComponent().setContext(popupMenu.getId());
        }
    }

    @Override
    public String getDisplay()
    {

        String display = null;
        ListItem selItem = getComponent().getSelectedItem();
        if (selItem != null)
        {
        	display = selItem.getLabel();
        }
        return display;
    }

    @Override
    public Object getValue()
    {
        Object retVal = null;
        ListItem selItem = getComponent().getSelectedItem();
        if (selItem != null)
        {
            retVal = selItem.getValue();
        }
        return retVal;
    }
    
    public void setValue(Object value)
    {
    	if (value != null && (value instanceof Integer || value instanceof String))
        {
            getComponent().setValue(value);
            
            if (getComponent().getSelectedIndex() == -1 && lookup != null)
            {
            	if (isReadWrite())
            		lookup.refresh();
                oldValue = value;
                refreshList();
            }
        }
        else
        {
            getComponent().setValue(null);
        }
        
        oldValue = value;
    }
    
    @Override
	public Listbox getComponent() {
		return (Listbox) component;
	}

	@Override
	public boolean isReadWrite() {
		return getComponent().isEnabled();
	}

	@Override
	public void setReadWrite(boolean readWrite) {
		getComponent().setEnabled(readWrite);
	}

	private void refreshList()
    {
    	if (getComponent().getItemCount() > 0)
    		getComponent().removeAllItems();

    	if (isReadWrite())
    	{
	        if (lookup != null)
	        {
	            int size = lookup.getSize();
	            
	            for (int i = 0; i < size; i++)
	            {
	                Object obj = lookup.getElementAt(i);
	                if (obj instanceof KeyNamePair)
	                {
	                    KeyNamePair lookupKNPair = (KeyNamePair) obj;
	                    getComponent().appendItem(lookupKNPair.getName(), lookupKNPair.getKey());
	                }
	                else if (obj instanceof ValueNamePair)
	                {
	                    ValueNamePair lookupKNPair = (ValueNamePair) obj;
	                    getComponent().appendItem(lookupKNPair.getName(), lookupKNPair.getValue());
	                }
	            }
	        }	        	        
    	}
    	else
    	{
    		if (lookup != null)
	        {
    			lookup.removeAllElements();
    			NamePair pair = lookup.getDirect(oldValue, false, false);
    			if (pair instanceof KeyNamePair) {
    				int key = ((KeyNamePair)pair).getKey();
    				getComponent().appendItem(pair.getName(), key);
    			} else if (pair instanceof ValueNamePair) {
    				ValueNamePair valueNamePair = (ValueNamePair) pair;
                    getComponent().appendItem(valueNamePair.getName(), valueNamePair.getValue());
    			}
	        }
    	}
    	getComponent().setValue(oldValue);
    }
    
    public void onEvent(Event event)
    {
        Object newValue = getValue();
        ValueChangeEvent changeEvent = new ValueChangeEvent(this, this.getColumnName(), oldValue, newValue);
        super.fireValueChange(changeEvent);
        oldValue = newValue;
    }
    
    public String[] getEvents()
    {
        return LISTENER_EVENTS;
    }

    public void contentsChanged(ListDataEvent e)
    {
        refreshList();
    }

    public void intervalAdded(ListDataEvent e)
    {}

    public void intervalRemoved(ListDataEvent e)
    {}
    
    public WEditorPopupMenu getPopupMenu()
    {
    	return popupMenu;
    }
    
    public void actionRefresh()
    {
    	Object curValue = getValue();
		if (lookup != null)
        {
			if (isReadWrite())
				lookup.refresh();
            refreshList();
		    setValue(curValue);
        }
    }
    
    /* (non-Javadoc)
	 * @see org.adempiere.webui.editor.IZoomableEditor#actionZoom()
	 */
    public void actionZoom()
	{
    	AEnv.actionZoom(lookup, getValue());
	}	
    
	public void onMenu(ContextMenuEvent evt) 
	{
		if (WEditorPopupMenu.REQUERY_EVENT.equals(evt.getContextEvent()))
		{
			actionRefresh();
		}
		else if (WEditorPopupMenu.ZOOM_EVENT.equals(evt.getContextEvent()))
		{
			actionZoom();
		}
		else if (WEditorPopupMenu.PREFERENCE_EVENT.equals(evt.getContextEvent()))
		{
			if (MRole.getDefault().isShowPreference())
				ValuePreference.start (this.getGridField(), getValue());
			return;
		}
	}
	
	public  void propertyChange(PropertyChangeEvent evt)
	{
		if ("FieldValue".equals(evt.getPropertyName()))
		{
			setValue(evt.getNewValue());
		}
	}
	
	@Override
	public void dynamicDisplay()
    {    	
		if (isReadWrite() && (lookup != null) && (!lookup.isValidated() || !lookup.isLoaded()))
			this.actionRefresh();
    }
}
