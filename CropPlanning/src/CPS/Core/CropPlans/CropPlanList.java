/* CropDBCropList.java - Created: March 14, 2007
 * Copyright (C) 2007, 2008 Clayton Carter
 * 
 * This file is part of the project "Crop Planning Software".  For more
 * information:
 *    website: http://cropplanning.googlecode.com
 *    email:   cropplanning@gmail.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package CPS.Core.CropPlans;

import CPS.Data.CPSComplexPlantingFilter;
import CPS.Data.CPSCrop;
import CPS.Data.CPSRecord;
import CPS.UI.Modules.CPSMasterView;
import CPS.Module.*;
import CPS.Data.CPSPlanting;
import CPS.UI.Modules.CPSMasterDetailModule;
import CPS.UI.Swing.CPSComplexFilterDialog;
import CPS.UI.Swing.CPSTable.CPSComboBoxCellEditor;
import CPS.UI.Swing.autocomplete.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.table.*;

// package access
class CropPlanList extends CPSMasterView implements ActionListener,
                                                    ItemListener {
   
   protected static final String KEY_FILTER = "LAST_FILTER";
   
    private JLabel lblPlanName;
//    private JComboBox cmbxPlanList;
    private PlanManager planMan;
    private JComboBox cmbxCropList, cmbxFieldList;
    private JButton btnChangePlans;
    
    private JComboBox cmbxLimit;
    protected static final String LIMIT_ALL       = "all";
    protected static final String LIMIT_ALL_UNP   = "all: not planted";
    protected static final String LIMIT_THIS_WEEK = "all: this week";
    protected static final String LIMIT_NEXT_WEEK = "all: next week";
    protected static final String LIMIT_THIS_NEXT = "all: this and next week";
    protected static final String LIMIT_DS        = "just DS";
    protected static final String LIMIT_DS_UNP    = "just DS: not planted";
    protected static final String LIMIT_TP        = "just TP";
    protected static final String LIMIT_TP_NOT_S  = "just TP: not seeded";
    protected static final String LIMIT_TP_NOT_P  = "just TP: seeded, not planted";
    protected static final String LIMIT_CUSTOM    = "custom..."; // TODO, this is a terrible name
    private JButton btnLimit;
    private CPSComplexFilterDialog dlgFilter;
    
//    private ArrayList<String> listOfValidCropPlans;
    private ArrayList<String> listOfValidCrops, listOfFields;
    
    public CropPlanList( CPSMasterDetailModule mdm ) {
        super(mdm);
        setSortColumn( CPSDataModelConstants.PROP_DATE_PLANT );
        filter = new CPSComplexPlantingFilter();
        dlgFilter = new CPSComplexFilterDialog();
//        loadPrefs();
        planMan = new PlanManager();
    }
    
    
   @Override
    public void setDataSource( CPSDataModel dm ) {
       
      super.setDataSource(dm);
      
      planMan.setDataModel(dm);
      String plan = getPrefs().get( KEY_DISPLAYED_TABLE, "" );
      if ( !plan.equals( "" ) )
         planMan.selectPlan( plan );
        
      cmbxLimit.setSelectedItem( getPrefs().get( KEY_FILTER, LIMIT_ALL ) );
      
      dataUpdated();
      
      updateListOfCrops();
      updateListOfFieldNames();
      
   }

   @Override
   protected int saveState() {
      
      String selectedFilter = (String) cmbxLimit.getSelectedItem();
      if ( ! selectedFilter.equals( LIMIT_CUSTOM ))
         getPrefs().put( KEY_FILTER, selectedFilter );
         
      return super.saveState();
   }

   @Override
   protected int getTypeOfDisplayedRecord() {
      return CPSDataModelConstants.RECORD_TYPE_PLANTING;
   }
   
    
    /** 
     * Retrieve the details (as a Planting) for a given ID 
     * @param id The integer id of the planting to retrieve.
     * @return The CPSPlanting object representing the retrieved record.
     */
    protected CPSPlanting getDetailsForID( int id ) {
       return getDataSource().getPlanting( getDisplayedTableName(), id );
    }

    protected CPSPlanting getDetailsForIDs( ArrayList<Integer> ids ) {
       return getDataSource().getCommonInfoForPlantings( getDisplayedTableName(), ids );
    }
    
    
    
    /**
     * Populates the crop name combobox with the list of valid crop names from CropDB.
     */
    protected void updateListOfCrops() {
       if ( ! isDataAvailable() )
          return;
       
       listOfValidCrops = getDataSource().getCropNameList();
       Collections.sort( listOfValidCrops, String.CASE_INSENSITIVE_ORDER);
       cmbxCropList.removeAllItems();
       cmbxCropList.addItem("");
       for ( String s : listOfValidCrops )
          cmbxCropList.addItem(s);
       
    }
    
    protected void updateListOfFieldNames() {
       if ( ! isDataAvailable() )
          return;
       
        listOfFields = getDataSource().getFieldNameList( getSelectedPlanName() );
       Collections.sort( listOfFields, String.CASE_INSENSITIVE_ORDER);
       cmbxFieldList.removeAllItems();
       cmbxFieldList.addItem("");
       for ( String s : listOfFields )
          cmbxFieldList.addItem(s);
        
    }
    
    protected void updateMasterList() {
       super.updateMasterList();

       if ( masterTable.getRowCount() > 0 ) 
           // install custom table renderes and editors
           for ( int i = 0; i < masterTable.getColumnModel().getColumnCount(); i++ ) {
               // install autocomplete combobox in column "crop_name"
               if ( masterTable.getColumnName( i ).equalsIgnoreCase( "crop_name" ) ) {
                   masterTable.getColumnModel().getColumn( i ).setCellEditor( new CPSComboBoxCellEditor( cmbxCropList ) );
                   continue;
               }
               if ( masterTable.getColumnName( i ).equalsIgnoreCase( "location" ) ) {
                   masterTable.getColumnModel().getColumn( i ).setCellEditor( new CPSComboBoxCellEditor( cmbxFieldList ) );
                   continue;
               }
           }
    }
       
       
    protected TableModel getMasterListData() {
        if ( !isDataAvailable() )
            return new DefaultTableModel();
         
        String selectedPlan = getSelectedPlanName();       
//        if ( selectedPlan != null && listOfValidCropPlans.contains( selectedPlan ) )
        if ( selectedPlan != null )
            return getDataSource().getCropPlan( selectedPlan, getDisplayedColumnList(), getSortColumn(),
                                                (CPSComplexPlantingFilter) getFilter() );
//            return getDataSource().getCropPlan( selectedPlan, getSortColumn(), getFilterString() );
       else
          // TODO error checking fall through to following call when invalid plan is selected
          return new DefaultTableModel();
       
    }
    
//    protected String getDisplayedTableName() { return (String) cmbxPlanList.getSelectedItem(); }
    protected String getDisplayedTableName() { return planMan.getSelectedPlanName(); }
    String getSelectedPlanName() { return getDisplayedTableName(); }
    
    protected void buildAboveListPanel() {
        initAboveListPanel();
                
        lblPlanName = new JLabel( "Plan Name: " );
        btnChangePlans = new JButton( "Change Plans" );
        btnChangePlans.setActionCommand( "ChangePlans" );
        btnChangePlans.setMargin( new Insets( 1, 5, 1, 5 ) );
        btnChangePlans.addActionListener( this );
       
        jplAboveList.add( lblPlanName );
        jplAboveList.add( btnChangePlans );
        
        // false ==> do not initialize panel
        super.buildAboveListPanel(false);
    }
    
    protected void buildListPanel() {
       super.buildListPanel();
       
       cmbxCropList = new JComboBox();
       cmbxCropList.addActionListener( new CropBoxInTableActionListener() );
       AutoCompleteDecorator.decorate( cmbxCropList );
       
       cmbxFieldList = new JComboBox();
       cmbxFieldList.setEditable(true);
       AutoCompleteDecorator.decorate(cmbxFieldList);
       
       
       
    }

    @Override
    protected JPanel buildFilterComponent() {
        initFilterPanel();
        
//        btnLimit = new JButton( "Limit View" );
//        btnLimit.setMargin( new Insets( 1, 1, 1, 1 ));
//        btnLimit.addActionListener(this);
        
        cmbxLimit = new JComboBox();
        cmbxLimit.setMaximumRowCount(15);
        cmbxLimit.addItem( LIMIT_ALL );
        cmbxLimit.addItem( LIMIT_ALL_UNP );
        cmbxLimit.addItem( LIMIT_THIS_WEEK );
        cmbxLimit.addItem( LIMIT_NEXT_WEEK );
        cmbxLimit.addItem( LIMIT_THIS_NEXT );
        cmbxLimit.addItem( LIMIT_DS );
        cmbxLimit.addItem( LIMIT_DS_UNP );
        cmbxLimit.addItem( LIMIT_TP );
        cmbxLimit.addItem( LIMIT_TP_NOT_S  );
        cmbxLimit.addItem( LIMIT_TP_NOT_P  );
        cmbxLimit.addItem( LIMIT_CUSTOM );
        cmbxLimit.setMaximumSize( cmbxLimit.getPreferredSize() );
        cmbxLimit.addItemListener(this);
        cmbxLimit.addActionListener(this);
                
        jplFilter.add( new JLabel( "Show" ));
//        jplFilter.add( btnLimit );
        jplFilter.add( cmbxLimit );
        jplFilter.add( new JLabel( "matching"));
        
        return super.buildFilterComponent( false );
    }

    @Override
    protected void updateFilter() {
        filter = dlgFilter.getFilter();
        super.updateFilter();
    }
    
    
    public class CropPlanBoxActionListener implements ActionListener {
      public void actionPerformed( ActionEvent actionEvent ) {
         String action = actionEvent.getActionCommand();

         System.out.println( "DEBUG Action performed in CropPlanList: " + action );

         if ( action.equalsIgnoreCase( "comboBoxEdited" ) ) {
         // What do we do here? create a new plan?
         }
         else if ( action.equalsIgnoreCase( "comboBoxChanged" ) ) {
            updateMasterList();
         }
       }   
    }
    public class CropBoxInTableActionListener implements ActionListener {
      public void actionPerformed( ActionEvent actionEvent ) {
         String action = actionEvent.getActionCommand();

         CPSModule.debug( "CropBoxInTable", "Action registered: " + action );

         if ( action.equalsIgnoreCase( "comboBoxEdited" ) ) {
         // What do we do here? create a new crop?
         }
         else if ( action.equalsIgnoreCase( "comboBoxChanged" ) ) {
            String s = (String) cmbxCropList.getSelectedItem();
            // update mesh this planting w/ the crop selected, but for now just ...
//            CPSModule.debug( "CropBoxInTable", "Crop name: selected " + s );
            if ( listOfValidCrops.contains( s ) ) {
               CPSCrop crop = getDataSource().getCropInfo( (String) cmbxCropList.getSelectedItem() );
//               CPSModule.debug( "CropBoxInTable", "Retrieved info for crop " + crop.getCropName() );
            }
            else {
               // TODO handle selection of non-existing crop
               CPSModule.debug( "CropBoxInTable", "ERROR: Selected non existent crop, what to do?  Create?  For now, nothing." );
            }
         }
       }   
    }
        
    @Override
    public CPSPlanting createNewRecord() {
       if ( getSelectedPlanName().equals("") ) {
          System.err.println("ERROR cannot create record unless a crop plan is selected");
          return null;
       }
       return getDataSource().createPlanting( getSelectedPlanName(), new CPSPlanting() );
    }
    
    @Override
    public CPSRecord duplicateRecord( int id ) {
        return getDataSource().createPlanting( getSelectedPlanName(),
                                         getDataSource().getPlanting( getSelectedPlanName(),
                                                                id ) );     
    }
    
    @Override
    public void deleteRecord( int id ) {
        getDataSource().deletePlanting( getSelectedPlanName(), id );
    }
    
   @Override
   protected ArrayList<String> getDisplayableColumnList() {
      return getDataSource().getPlantingDisplayablePropertyNames();
   }
   
   @Override
   protected ArrayList<Integer> getDefaultDisplayableColumnList() {
      return getDataSource().getPlantingDefaultProperties();
   }
   
   protected ArrayList<String[]> getColumnPrettyNameMap() {
       return getDataSource().getPlantingPrettyNames();
   }

    @Override
    protected String getTableStatisticsString() {
        if ( ! isDataAvailable() || getSelectedPlanName() == null )
            return "";
        
       CPSPlanting p = getDataSource().getSumsForCropPlan( getSelectedPlanName(),
                                                           (CPSComplexPlantingFilter) getFilter() );
       String s = "";
       
       if ( masterTable.getRowCount() > 0 ) {
           s += "Plantings:" + masterTable.getRowCount();
       
           String t = p.getBedsToPlantString();
           if ( ! t.equals("") ) 
               s += "/Beds:" + t;
           
           t = p.getRowFtToPlantString();
           if ( ! t.equals("") ) 
               s += "/RowFeet:" + t;
           
//       s += "/Plants:" + p.getPlantsNeededString();
           
           t = p.getFlatsNeededString();
           if ( ! t.equals("") )
               s += "/Flats:" + t;
           
           t = p.getTotalYieldString();
           if ( ! t.equals("") )
               s += "/Yield:" + t;
       }
       
       return s;
    }
    
    protected void updateSelectedPlanLabel() {
       String plan = getSelectedPlanName();
       
       if ( plan == null )
          plan = "No Plan Selected!";
       
       lblPlanName.setText( "Plan Name: " + plan + "  " );
    }
    
    @Override
    public void dataUpdated() {
        super.dataUpdated();
        
        updateSelectedPlanLabel();
        
        if ( getSelectedPlanName() == null ) {
            setStatus( "No plan selected.  Select a plan to display or use \"New Plan\" button to create a new one." );
        }
        else {
            updateListOfFieldNames();
        }
        
        
    }

    protected void updateAutocompletionComponents() {
       updateListOfCrops();
       updateListOfFieldNames();
   }
    
    
    public void itemStateChanged( ItemEvent arg0 ) {
        Object source = arg0.getSource();
        
        if ( source == cmbxLimit ) {
            CPSModule.debug( "CropPlanList", "view limit combobox changed to: " + cmbxLimit.getSelectedItem() );
            String s = (String) cmbxLimit.getSelectedItem();
            if      ( s.equalsIgnoreCase( LIMIT_ALL_UNP ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.allNotPlantedFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_THIS_WEEK ))
               dlgFilter.setFilter( CPSComplexPlantingFilter.thisWeekFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_NEXT_WEEK ))
               dlgFilter.setFilter( CPSComplexPlantingFilter.nextWeekFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_THIS_NEXT ))
               dlgFilter.setFilter( CPSComplexPlantingFilter.thisAndNextWeekFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_DS ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.directSeededFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_DS_UNP ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.DSNotPlantedFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_TP ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.transplantedFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_TP_NOT_S  ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.TPNotSeededFilter() );
            else if ( s.equalsIgnoreCase( LIMIT_TP_NOT_P  ))
                dlgFilter.setFilter( CPSComplexPlantingFilter.TPSeededNotPlantedFilter() );
//            else if ( s.equalsIgnoreCase( LIMIT_CUSTOM ))
                 // ignore this; just let the action listener handle it
//                dlgFilter.setVisible( true );
            else // if ( s.equalsIgnoreCase( LIMIT_ALL ))
                dlgFilter.resetFilter();
            updateFilter();
        }
    }
    
    
    public void actionPerformed(ActionEvent actionEvent) {
        String action = actionEvent.getActionCommand();

        CPSModule.debug( "CropPlanList", "Action performed in CropPlanList: " + action );

       if ( action.equalsIgnoreCase( btnChangePlans.getActionCommand() ) ) {
            if ( ! isDataAvailable() ) {
                CPSModule.debug( "CropPlanList", "ERROR: cannot create new plan, no data available" );
                return;
            }
//            createNewCropPlan( getSelectedPlanName() );
            String s = planMan.getSelectedPlanName();
            planMan.setVisible(true);
//            if ( ! s.equalsIgnoreCase( planMan.getSelectedPlanName() ) ) {
//               lblPlanName.setText( "Plan Name: " + planMan.getSelectedPlanName() );
               dataUpdated();
//       }
        }
       else if ( actionEvent.getSource() == cmbxLimit &&
                ((String) cmbxLimit.getSelectedItem()).equalsIgnoreCase( LIMIT_CUSTOM ) ) {
           dlgFilter.setVisible( true );
           if ( dlgFilter.getFilter().isViewLimited() )
               updateFilter();
           else
               cmbxLimit.setSelectedItem( LIMIT_ALL );
       }
        else
            super.actionPerformed( actionEvent );    
        
    }
    
    
}
