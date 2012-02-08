/* HSQLDBCreator.java - Created: February 1, 2007
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

package CPS.Core.DB;

import CPS.Data.CPSCrop;
import CPS.Data.CPSDatum;
import CPS.Data.CPSPlanting;
import CPS.Data.CPSRecord;
import CPS.Module.CPSDataModelConstants;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Clayton
 */
public class HSQLDBCreator {
   
   // private constructor == no instantiation
   private HSQLDBCreator() {}
   
   // package level access
   static void createTables( Connection con, long currentVersion ) {
      
      try {
         Statement st = con.createStatement();
      
         st.executeUpdate( createTableDBMetaData() );
         setLastUpdateVersion( con, currentVersion );
         
         st.executeUpdate( createTableCropPlans() );
         st.executeUpdate( createTableCropsAndVarieties() );
//         st.executeUpdate( createTablePlantingMethods() );
         
         st.close();
         
//         createCropPlan( con, "COMMON_PLANTINGS" );
      }
      catch ( SQLException e ) { e.printStackTrace(); }
         
   }
   
   protected static String createTableDBMetaData() {
       return statementCreateTable( "CPS_METADATA", HSQLDBSchemas.cpsDbMetaDataSchema() ) + "; " +
              "INSERT INTO CPS_METADATA ( prev_ver ) VALUES ( 0 )";
   }
   
   static void createCropPlan( Connection con, String name, int year, String desc ) {
      
      // TODO error if plan with name already exists
      
      try {
         Statement st = con.createStatement();
   
         // Create the table for the crop plan
         String s = createTableCropPlan( name );
         System.out.println("Executing update: " + s );
         
         st.executeUpdate( createTableCropPlan( name ) );
      
         // Record the plan in the table listing all of the plans.
         s = "INSERT INTO CROP_PLANS( plan_name ) VALUES( " + HSQLDB.escapeValue( name ) + " );";
      
         System.out.println("Executing update: " + s );
         st.executeUpdate( s );
         st.close();
      }
      catch ( SQLException e ) { e.printStackTrace(); }      
      
      updateCropPlan( con, name, year, desc );
   }
   
   static void updateCropPlan( Connection con, String name, int year, String desc ) {

      try {
         Statement st = con.createStatement();
      
         // Update the plan metadata
         String s = "UPDATE " + HSQLDB.escapeTableName( "CROP_PLANS" ) +
                 " SET year = " + year + ", description = " +  HSQLDB.escapeValue( desc ) +
                 " WHERE plan_name = " + HSQLDB.escapeValue( name );
      
         System.out.println("Executing update: " + s );
         st.executeUpdate( s );
         st.close();
      }
      catch ( SQLException e ) { e.printStackTrace(); }
   
   }
   
   static void deleteCropPlan( Connection con, String name ) {

      try {
         Statement st = con.createStatement();
   
         // Drop the table
         String s = "DROP TABLE " + HSQLDB.escapeTableName( name );
         System.out.println( "Executing update: " + s );
         
         st.executeUpdate( s );

         // Remove the table record from the plan metadata table
         s = "DELETE FROM CROP_PLANS WHERE plan_name = " + HSQLDB.escapeValue( name );
      
         System.out.println( "Executing update: " + s );
         st.executeUpdate( s );
         st.close();
      }
      catch ( SQLException e ) { e.printStackTrace(); }      
   
   }
   
   public static void deleteRecord( Connection con, String table, int row ) {
      
       try {
           String s = "DELETE FROM " + HSQLDB.escapeTableName(table) + " WHERE id = " + row;
      
           System.out.println("Executing update: " + s);
           Statement st = con.createStatement();
           st.executeUpdate(s);
           st.close();
      }
      catch ( SQLException e ) { e.printStackTrace(); }
       
   }
   
   private static String createTableCropPlans() {
      return statementCreateTable( "CROP_PLANS", HSQLDBSchemas.cropPlansListSchema() );
   }
   
   private static String createTableCropPlan( String name ) {
      return statementCreateTable( name, HSQLDBSchemas.cropPlanSchema() );
   }
   
   private static String createTableCropsAndVarieties() {    
      return statementCreateTable( "CROPS_VARIETIES", HSQLDBSchemas.cropAndVarietySchema() );
   }
   
   private static String createTablePlantingMethods() {
      return statementCreateTable( "PLANTING_METHODS", HSQLDBSchemas.plantingDataSchema() );
   }
   
   private static String statementCreateTable( String name, String table_def ) {
      // TODO: test if table_def ends with "," or ", " and strip it off
      if ( table_def.endsWith( "," ))
         table_def = table_def.substring( 0, table_def.length() - 1 );
      if ( table_def.endsWith( ", " ))
         table_def = table_def.substring( 0, table_def.length() - 2 );
      
      return "CREATE TABLE " + HSQLDB.escapeTableName( name ) + " ( " + table_def + " ) ";
   }
   
   
   /**
    * Updates the DB metadata table to record which program version was the last to update the database.  This
    * figure chould be different from the currect program version if there have been one or more program
    * releases made the last release which required a db structure update.  (eg, LastUpdateVersion could be
    * 1.0.2 while current version is 1.0.6)
    * This method should technically only be called when the db is first created and when explicit updates
    * to the db schema and/or structure are made.
    * @param con - JDBC Connection upon which to execute the version update statement.
    * @param version - long int version number to set
    */
   public static void setLastUpdateVersion( Connection con, long version ) {
       
      try {
         
         String sqlUpdate = "UPDATE " + HSQLDB.escapeTableName( "CPS_METADATA" ) + 
                            " SET prev_ver = " + version;
         
         System.out.println("Attempting to execute: " + sqlUpdate );
         Statement st = con.createStatement();
         st.executeUpdate( sqlUpdate );
         st.close();
         
      }
      catch ( SQLException ex ) { ex.printStackTrace(); }
       
   }
   
   
   
   public static int insertCrop( Connection con, HSQLColumnMap map, CPSCrop crop ) {
   
      try {
         
         String cols = "";
         String vals = "";
         
         Iterator<CPSDatum> i = crop.iterator();
         CPSDatum c;
         boolean isEmpty = crop.getID() == -1;
         
         while ( i.hasNext() ) {
            c = i.next();
            if ( c.isValid() || isEmpty ) {
               // System.out.println(" Processing datum: " + c.getColumnName() );
//               cols += c.getColumnName() + ", ";
               cols += map.getCropColumnNameForProperty( c.getPropertyNum() ) + ", ";
               if ( c.isInherited() )
                  vals += HSQLDB.escapeValue( "null" ) + ", ";
               else 
                  vals += HSQLDB.escapeValue( c.getDatum() ) + ", ";
            }
         }
         
//         cols += "similar_to";
//         vals += HSQLDB.escapeValue( crop.getSimilarCrop().getCropName() );

         if ( cols.length() > 0 && vals.length() > 0 ) {
            cols = cols.substring( 0, cols.lastIndexOf( ", " ));
            vals = vals.substring( 0, vals.lastIndexOf( ", " ));
         }
         
         //"Fudge","mat_adjust","misc_adjust","seeds_sources","seeds_item_codes","seeds_unit_size"         
         
         String sql = "INSERT INTO CROPS_VARIETIES ( " + cols + " ) VALUES ( " + vals + " )";
         
         System.out.println( "Attempting to execute: " + sql );
         
         Statement st = con.createStatement();
         if ( st.executeUpdate( sql ) == -1 )
            System.err.println( "Error creating crop " + crop.getCropName() );
         
         ResultSet rs = st.executeQuery( "CALL IDENTITY()" );
         rs.next();
         int newID = rs.getInt(1);
         rs.close();
         st.close();
         
         System.out.println( "Inserted " + crop.getCropName() + " with id " + newID );
         
         return newID;
         
      }
      catch ( SQLException ex ) { 
         ex.printStackTrace(); 
         return -1;
      }
   }
   
   public static void updateCrop( Connection con, HSQLColumnMap map, CPSCrop crop ) {
      ArrayList<Integer> id = new ArrayList<Integer>();
      id.add( new Integer( crop.getID() ));
      updateCrops( con, map, crop, id );
   }
   
   public static void updateCrops( Connection con, HSQLColumnMap map, CPSCrop changes, ArrayList<Integer> ids ) {
      updateRecords( con, CPSDataModelConstants.RECORD_TYPE_CROP, map, "CROPS_VARIETIES", changes, ids, null );
   }
   
   public static int insertPlanting( Connection con, 
                                     HSQLColumnMap map,
                                     String planName,
                                     CPSPlanting planting,
                                     int cropID ) {
   
      try {
         
         String cols = "";
         String vals = "";
         
         Iterator<CPSDatum> i = planting.iterator();
         CPSDatum c;
         boolean isEmpty = planting.getID() == -1;
         
         while ( i.hasNext() ) {
            c = i.next();
            if ( c.isConcrete() || isEmpty ) {
//            if ( c.isValid() || isEmpty ) {
               // System.out.println(" Processing datum: " + c.getColumnName() );
//               cols += c.getColumnName() + ", "; 
               cols += map.getPlantingColumnNameForProperty( c.getPropertyNum() ) + ", ";
               // This would be the place to check for data inheritance and insert "escape" data for it
               vals += HSQLDB.escapeValue(c.getDatum()) + ", ";
            }
         }

         if ( cols.length() > 0 && vals.length() > 0 ) {
            cols = cols.substring( 0, cols.lastIndexOf( ", " ));
            vals = vals.substring( 0, vals.lastIndexOf( ", " ));
         }
         
         if ( cropID != -1 ) {
             cols += ", crop_id";
             vals += ", " + cropID;
         }
         
         String sql = "INSERT INTO " + HSQLDB.escapeTableName( planName ) + " ( " + cols + " ) VALUES ( " + vals + " )";
         
         System.out.println( "Attempting to execute: " + sql );
         
         Statement st = con.createStatement();
         if ( st.executeUpdate( sql ) == -1 )
            System.err.println( "Error creating planting " + planting.getCropName() );
         
         ResultSet rs = st.executeQuery( "CALL IDENTITY()" );
         rs.next();
         int newID = rs.getInt(1);
         rs.close();
         st.close();
         
         System.out.println( "Inserted new planting " + planting.getCropName() + " with id " + newID );
         
         return newID;
         
      }
      catch ( SQLException ex ) { 
         ex.printStackTrace(); 
         return -1;
      }
   }

   /* TODO updatePlanting and updateCrop could be conflated into an updateRecord
    * method that takes a String tableName and a CPSRecord.  Everything else is
    * identical. Perhaps same thing for insertCrop and insertPlanting */
   public static void updatePlanting( Connection con, HSQLColumnMap map, String planName, CPSPlanting p, int cropID ) {
      ArrayList<Integer> id = new ArrayList();
      ArrayList<Integer> cID = new ArrayList();
      
      id.add( new Integer( p.getID() ) );
      cID.add( new Integer( cropID ) );
      
      updatePlantings( con, map, planName, p, id, cID );
   }
   
   public static void updatePlantings( Connection con, 
                                       HSQLColumnMap map,
                                       String planName,
                                       CPSPlanting changes, 
                                       ArrayList<Integer> ids,
                                       ArrayList<Integer> cropIDs ) {
      updateRecords( con, CPSDataModelConstants.RECORD_TYPE_PLANTING, map, planName, changes, ids, cropIDs );
   }
   
   /**
    * Update a list of (one or more) records.
    * 
    * @param con A connection upon which to perform the update.
    * @param tableName The table to update.
    * @param changes A "sparse" CPSRecord object containing the data to update.
    * @param changedIDs A list of record ids to update.
    * @param cropIDs A list ids from table CROPS_VARIETIES that correspond to the list of record ids.
    *                This may be "null" if the records being updated are CPSCrops, but MUST NOT be 
    *                "null" when CPSPlantings are being updated.  The list must correspond in length
    *                and order to the list of records to be updated.
    */
   private static void updateRecords( Connection con, 
                                      int recordType,
                                      HSQLColumnMap map,
                                      String tableName,
                                      CPSRecord changes, 
                                      ArrayList<Integer> changedIDs,
                                      ArrayList<Integer> cropIDs ) {
      // To the developer: a simpler structure for this method would be to just create one
      // UPDATE statement with the condition if "WHERE id IN ( changedIDs )", but we have to
      // do it this way in order to support the crop_id FOREIGN KEY in the crop plan schema
      
      try {
         
         // Build the list of changes to commit to each record
         String sqlChanges = "";
         Iterator<CPSDatum> iter = changes.iterator();
         CPSDatum c;
         while ( iter.hasNext() ) {
            c = iter.next();
            if ( c.isValid() ) {
               if ( recordType == CPSDataModelConstants.RECORD_TYPE_CROP )
                  sqlChanges += map.getCropColumnNameForProperty( c.getPropertyNum() );
               else // if ( recordType == CPSDataModelConstants.RECORD_TYPE_PLANTING )
                  sqlChanges += map.getPlantingColumnNameForProperty( c.getPropertyNum() );
                  
               sqlChanges += " = " + HSQLDB.escapeValue( c.getDatum() ) + ", ";
            }
         }
         sqlChanges = sqlChanges.substring( 0, sqlChanges.lastIndexOf( ", " ) );

         
         // Build the overall update statement for each record id
         String sqlUpdate = "";
         for ( int i = 0; i < changedIDs.size(); i++ ) {
       
            sqlUpdate += "UPDATE " + HSQLDB.escapeTableName( tableName ) + " SET " + sqlChanges;
            
            if ( changes instanceof CPSPlanting )
               sqlUpdate += ", crop_id = " + cropIDs.get(i).intValue() ;
         
            // the first space character on the following line is CRUCIAL
            sqlUpdate += " " + "WHERE id = " + changedIDs.get(i).intValue() + " ; ";
         
         }
         
         System.out.println("Attempting to execute: " + sqlUpdate );
         Statement st = con.createStatement();
         st.executeUpdate( sqlUpdate );
         st.close();
         
      }
      catch ( SQLException ex ) { ex.printStackTrace(); }
   }

}
