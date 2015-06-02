//holds instance attribute state for the federate
//used as the 'model' for the UI view (JTable)

package se.pitch.sushifederation.consumption;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.RTIexception;
import se.pitch.sushifederation.*;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public final class DinerTable extends AbstractTableModel
{

   //columns for this table
   public final static int POSITION = 0;
   public final static int STATE = 1;
   public final static int SERVING = 2;
   public final static int BOAT = 3;
   public final static int COL_COUNT = 4;

   //entry: Diner
   private Vector<Diner> _entries = new Vector<Diner>();

   private String[] _columnNames = {
         "Position",
         "State",
         "Serving",
         "Boat to Xfer from"};

   public synchronized ObjectInstanceHandle getBoatHandle(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getBoat();
   }

   public synchronized ObjectInstanceHandle getHandle(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getHandle();
   }

   public synchronized String getName(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getName();
   }

   public synchronized double getPosition(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getPosition().getAngle();
   }

   //return the diner serial that is looking for a given serving
   //-1 if not found
   public synchronized int getDinerForServing(ObjectInstanceHandle servingHandle) {
      int size = _entries.size();
      for (int i = 0; i < size; ++i) {
         Diner entry = _entries.elementAt(i);
         if (entry.getServing() != null && entry.getServing().equals(servingHandle)) {
            return i;
         }
      }
      return -1;
   }

   public synchronized Position getFullPosition(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      Position position = null;
      try {
         position = (Position)(entry.getPosition().clone());
      }
      catch (CloneNotSupportedException e) {
         // ignore
      }
      return position;
   }

   public synchronized Diner.State getState(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getState();
   }

   public synchronized ObjectInstanceHandle getServing(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getServing();
   }

   public synchronized String getServingName(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      return entry.getServingName();
   }

   public synchronized boolean isInReach(
         int serial,
         double angle,
         double reach)
   {
      Diner entry = _entries.elementAt(serial);
      double DinerAngle = entry.getPosition().getAngle();
      double difference = DinerAngle - angle;
      if (difference < 0.0) difference = -difference;
      if (difference > 180.0) difference = 360.0 -difference;
      return difference <= reach;
   }

   //are any diners in a state where a time advance is required?
   public synchronized boolean isTimeAdvanceRequired() {
      int count = _entries.size();
      for (int i = 0; i < count; ++i) {
         Diner diner = _entries.elementAt(i);
         if (diner.getState() == Diner.State.EATING || diner.getState() == Diner.State.LOOKING_FOR_FOOD)
            return true;
      }
      return false;
   }

   //add new Diner
   public synchronized void add(
         ObjectInstanceHandle handle,
         String name,
         double position,
         Diner.State state,
         String servingName,
         ObjectInstanceHandle serving)
   {
      Diner newEntry = new Diner();
      newEntry.setHandle(handle);
      newEntry.setName(name);
      newEntry.setPosition(new Position(position, OffsetEnum.OUTBOARD_CANAL));
      newEntry.setPositionState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setState(state);
      newEntry.setStateState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setServingName(servingName);
      newEntry.setServingNameState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setServing(serving);
      newEntry.setPrivilegeToDeleteObjectState(AttributeState.OWNED_INCONSISTENT);
      _entries.addElement(newEntry);
      int index = _entries.size() - 1;
      //tell TableView to update
      fireTableRowsInserted(index, index);
      fireTableRowsUpdated(index, index);
   }

   public synchronized void setPositionConsistent(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setPositionState(AttributeState.OWNED_CONSISTENT);
   }

   public synchronized void setState(int serial, Diner.State state)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setState(state);
      //state becomes inconsistent only on transition visible outside federate
      if (state == Diner.State.EATING || state == Diner.State.LOOKING_FOR_FOOD)
         entry.setStateState(AttributeState.OWNED_INCONSISTENT);
      fireTableRowsUpdated(serial, serial);
   }

   public synchronized void setStateConsistent(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setStateState(AttributeState.OWNED_CONSISTENT);
   }

   public synchronized void setBoatHandle(int serial, ObjectInstanceHandle boat)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setBoat(boat);
      fireTableRowsUpdated(serial, serial);
   }

   public synchronized void setServing(int serial, ObjectInstanceHandle serving)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setServing(serving);
   }

   public synchronized void setServingName(int serial, String servingName)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setServingName(servingName);
      entry.setServingNameState(AttributeState.OWNED_INCONSISTENT);
      fireTableRowsUpdated(serial, serial);
   }

   public synchronized void setServingNameConsistent(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Diner entry = _entries.elementAt(serial);
      entry.setServingNameState(AttributeState.OWNED_CONSISTENT);
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
      Diner entry = _entries.elementAt(row);
      if (col == POSITION) {
         return entry.getPosition().getAngle();
      } else if (col == STATE) {
         return entry.getState().getName();
      } else if (col == SERVING) {
         if (entry.getState() == Diner.State.EATING || entry.getState() == Diner.State.PREPARING_TO_DELETE_SERVING) {
            return entry.getServing();
         } else {
            return "";
         }
      }
      else if (col == BOAT) {
         if (entry.getState() == Diner.State.ACQUIRING) {
            return entry.getBoat();
         } else {
            return "";
         }
      }
      else return null;
   }

   public synchronized boolean isCellEditable(int row, int col) { return false; }

   public synchronized void setValueAt(Object value, int row, int col) {
      // our table isn't editable from UI
   }

   public synchronized void updateDiners(
         LogicalTime sendTime,
         AttributeHandleValueMapFactory attributeHandleValueMapFactory,
         AttributeHandle positionAttribute,
         AttributeHandle servingNameAttribute,
         AttributeHandle stateAttribute,
         RTIambassador rti)
         throws RTIexception
   {
      int count = _entries.size();
      for (int serial = 0; serial < count; ++serial) {
         Diner diner = (Diner)_entries.elementAt(serial);
         boolean needToUpdate = false;
         AttributeHandleValueMap sa = attributeHandleValueMapFactory.create(2);
         if (diner.getPositionState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(positionAttribute, diner.getPosition().encode());
            needToUpdate = true;
            diner.setPositionState(AttributeState.OWNED_CONSISTENT);
         }
         if (diner.getServingNameState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(servingNameAttribute, InstanceName.encode(diner.getServingName()));
            needToUpdate = true;
            diner.setServingNameState(AttributeState.OWNED_CONSISTENT);
         }
         if (diner.getStateState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(stateAttribute, IntegerAttribute.encode(diner.getState().getOrdinal()));
            needToUpdate = true;
            diner.setStateState(AttributeState.OWNED_CONSISTENT);
         }
         if (needToUpdate) {
            rti.updateAttributeValues(diner.getHandle(), sa, null, sendTime);
         }
      }
   }
}

