package se.pitch.sushifederation.manager;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public final class FederateTable extends AbstractTableModel
{
   private final class FederateEntry {
      private ObjectInstanceHandle _MOMhandle;
      private String _instanceName;
      private String _federateHandle;
      private int _federateHandleState;
      private String _federateType;
      private int _federateTypeState;
      private String _federateHost;
      private int _federateHostState;

      public ObjectInstanceHandle getMOMhandle() {
         return _MOMhandle;
      }

      public void setMOMhandle(ObjectInstanceHandle MOMhandle) {
         _MOMhandle = MOMhandle;
      }

      public String getInstanceName() {
         return _instanceName;
      }

      public void setInstanceName(String instanceName) {
         _instanceName = instanceName;
      }

      public String getFederateHandle() {
         return _federateHandle;
      }

      public void setFederateHandle(String federateHandle) {
         _federateHandle = federateHandle;
      }

      public int getFederateHandleState() {
         return _federateHandleState;
      }

      public void setFederateHandleState(int federateHandleState) {
         _federateHandleState = federateHandleState;
      }

      public String getFederateType() {
         return _federateType;
      }

      public void setFederateType(String federateType) {
         _federateType = federateType;
      }

      public int getFederateTypeState() {
         return _federateTypeState;
      }

      public void setFederateTypeState(int federateTypeState) {
         _federateTypeState = federateTypeState;
      }

      public String getFederateHost() {
         return _federateHost;
      }

      public void setFederateHost(String federateHost) {
         _federateHost = federateHost;
      }

      public int getFederateHostState() {
         return _federateHostState;
      }

      public void setFederateHostState(int federateHostState) {
         _federateHostState = federateHostState;
      }
   }

   public final class BarrierAlreadySet extends Exception {
   }

   public final static int OWNED = 1;
   public final static int DISCOVERED = 2;      //discovered but no data available
   public final static int REFLECTED = 3;       //data available
   public final static int NOT_REFLECTED = 4;   //not owned, not subscribed

   //columns for this table
   public final static int   FEDERATE_HANDLE = 0;
   public final static int   FEDERATE_TYPE = 1;
   public final static int   FEDERATE_HOST = 2;
   public final static int COL_COUNT = 3;

   private Vector _entries = new Vector();
   private String[] _columnNames = {
         "Federate Handle",
         "Federate Type",
         "Federate Host"};

   //add new FederateEntry
   public synchronized void add(ObjectInstanceHandle momHandle, String instanceName)
   {
      FederateEntry newEntry = new FederateEntry();
      newEntry.setMOMhandle(momHandle);
      newEntry.setInstanceName(instanceName);
      newEntry.setFederateHandleState(DISCOVERED);
      newEntry.setFederateTypeState(DISCOVERED);
      newEntry.setFederateHostState(DISCOVERED);
      _entries.addElement(newEntry);
      int index = _entries.size() - 1;
      //tell TableView to update
      fireTableRowsInserted(index, index);
      fireTableRowsUpdated(index, index);
   }

   public synchronized void remove(ObjectInstanceHandle momHandle) {
      //gotta go thru this rigmarole because entries shift upon deletion
      int index = -1;
      for (int i = 0; i < _entries.size(); ++i) {
         FederateEntry entry = (FederateEntry)_entries.elementAt(i);
         if (entry.getMOMhandle().equals(momHandle)) {
            index = i;
            break;
         }
      }
      if (index < 0) return;
      _entries.removeElementAt(index);
      fireTableDataChanged();
   }

   public synchronized String getColumnName(int col) {
      return _columnNames[col];
   }

   public synchronized int getRowCount() {
      return _entries.size();
   }

   public synchronized int getColumnCount() {
      return COL_COUNT;
   }

   public synchronized Object getValueAt(int row, int col) {
      if(_entries.size() > row) {
         FederateEntry entry = (FederateEntry)(_entries.elementAt(row));
         if (col == FEDERATE_HANDLE)
            return (entry.getFederateHandleState() == REFLECTED) ? entry.getFederateHandle() : "";
         else if (col == FEDERATE_TYPE)
            return (entry.getFederateTypeState() == REFLECTED) ? entry.getFederateType() : "";
         else if (col == FEDERATE_HOST)
            return (entry.getFederateHostState() == REFLECTED) ? entry.getFederateHost() : "";
      }
      return null;
   }

   public synchronized boolean isCellEditable(int row, int col) { return false; }

   public synchronized void setFederateHandle(ObjectInstanceHandle momHandle, String federateHandle) throws RTIexception
   {
      //find entry
      int index = -1;
      FederateEntry entry = null;
      for (int i = 0; i < _entries.size(); ++i) {
         entry = (FederateEntry)(_entries.elementAt(i));
         if (entry.getMOMhandle().equals(momHandle)) {
            index = i;
            break;
         }
      }
      if (index < 0) throw new RTIexception("object " + momHandle + " not known");
      entry.setFederateHandle(federateHandle);
      entry.setFederateHandleState(REFLECTED);
      fireTableCellUpdated(index, FEDERATE_HANDLE);
   }

   public synchronized void setFederateHost(ObjectInstanceHandle momHandle, String federateHost) throws RTIexception
   {
      //find entry
      int index = -1;
      FederateEntry entry = null;
      for (int i = 0; i < _entries.size(); ++i) {
         entry = (FederateEntry)(_entries.elementAt(i));
         if (entry.getMOMhandle().equals(momHandle)) {
            index = i;
            break;
         }
      }
      if (index < 0) throw new RTIexception("object " + momHandle + " not known");
      entry.setFederateHost(federateHost);
      entry.setFederateHostState(REFLECTED);
      fireTableCellUpdated(index, FEDERATE_HOST);
   }

   public synchronized void setFederateType(ObjectInstanceHandle momHandle, String federateType)
         throws RTIexception
   {
      //find entry
      int index = -1;
      FederateEntry entry = null;
      for (int i = 0; i < _entries.size(); ++i) {
         entry = (FederateEntry)(_entries.elementAt(i));
         if (entry.getMOMhandle().equals(momHandle)) {
            index = i;
            break;
         }
      }
      if (index < 0) throw new RTIexception("object " + momHandle + " not known");
      entry.setFederateType(federateType);
      entry.setFederateTypeState(REFLECTED);
      fireTableCellUpdated(index, FEDERATE_TYPE);
   }

   public synchronized void setValueAt(Object value, int row, int col) {
      // our table isn't editable from UI
   }

   public synchronized void updateFederates(
         RTIambassador rti,
         AttributeHandleSet federateAttributesSet)
   {
      try {
         int count = _entries.size();
         for (int index = 0; index < count; ++index) {
            FederateEntry entry = (FederateEntry)_entries.elementAt(index);
            if (entry.getFederateHandleState() == DISCOVERED
                  || entry.getFederateTypeState() == DISCOVERED
                  || entry.getFederateHostState() == DISCOVERED) {
               rti.requestAttributeValueUpdate(entry.getMOMhandle(), federateAttributesSet, new byte[0]);
            }
         }
      }
      catch (RTIexception e) {
         //ignore
      }
   }
}
