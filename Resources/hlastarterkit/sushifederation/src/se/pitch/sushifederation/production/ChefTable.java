package se.pitch.sushifederation.production;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.RTIexception;
import se.pitch.sushifederation.*;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

/**
 * holds instance attribute state for the federate
 * used as the 'model' for the UI view (JTable)
 */
public final class ChefTable extends AbstractTableModel {

   //columns for this table
   public final static int POSITION = 0;
   public final static int STATE = 1;
   public final static int SERVING = 2;
   public final static int BOAT = 3;
   public final static int COL_COUNT = 4;

   private Vector<Chef> _entries = new Vector<Chef>();

   private String[] _columnNames = {
         "Position",
         "State",
         "Serving",
         "Boat to Xfer to"};


   public synchronized ObjectInstanceHandle getBoatHandle(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      return entry.getBoat();
   }

   public synchronized ObjectInstanceHandle getHandle(int serial) throws ArrayIndexOutOfBoundsException {
      Chef entry = _entries.elementAt(serial);
      return entry.getHandle();
   }

   public synchronized double getPosition(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      return entry.getPosition().getAngle();
   }

   public synchronized Position getFullPosition(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      Position posn = null;
      try {
         posn = (Position)(entry.getPosition().clone());
      } catch (CloneNotSupportedException e) {
         //ignore
      }
      return posn;
   }

   public synchronized Chef.State getState(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      return entry.getState();
   }

   public synchronized ObjectInstanceHandle getServing(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      return entry.getServing();
   }

   public synchronized String getServingName(int serial)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      return entry.getServingName();
   }

   public synchronized boolean isInReach(
         int serial,
         double angle,
         double reach)
   {
      Chef entry = _entries.elementAt(serial);
      double chefAngle = entry.getPosition().getAngle();
      double difference = chefAngle - angle;
      if (difference < 0.0) difference = -difference;
      if (difference > 180.0) difference = 360.0 -difference;
      return difference <= reach;
   }

   //add new Chef
   public synchronized void add(
         ObjectInstanceHandle handle,
         String name,
         double position,
         Chef.State state,
         String servingName,
         ObjectInstanceHandle serving)
   {
      Chef newEntry = new Chef();
      newEntry.setHandle(handle);
      newEntry.setName(name);
      newEntry.setPosition(new Position(position, OffsetEnum.INBOARD_CANAL));
      newEntry.setPositionState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setState(state);
      newEntry.setStateState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setServingName(servingName);
      newEntry.setServingNameState(AttributeState.OWNED_INCONSISTENT);
      newEntry.setServing(serving);
      newEntry.setBoat(null);
      newEntry.setPrivilegeToDeleteObjectState(AttributeState.OWNED_INCONSISTENT);
      _entries.addElement(newEntry);
      int index = _entries.size() - 1;
      //tell TableView to update
      fireTableRowsInserted(index, index);
      fireTableRowsUpdated(index, index);
   }

   //return the chef serial that made a given serving
   //-1 if not found
   public synchronized int getChefForServing(ObjectInstanceHandle servingHandle) {
      int size = _entries.size();
      for (int i = 0; i < size; ++i) {
         Chef entry = _entries.elementAt(i);
         if (entry.getServing() != null && entry.getServing().equals(servingHandle)) return i;
      }
      return -1;
   }

   public synchronized void setState(int serial, Chef.State state)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      entry.setState(state);
      entry.setStateState(AttributeState.OWNED_INCONSISTENT);
      fireTableRowsUpdated(serial, serial);
   }

   public synchronized void setBoatHandle(int serial, ObjectInstanceHandle boat)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      entry.setBoat(boat);
      fireTableRowsUpdated(serial, serial);
   }

   public synchronized void setServing(int serial, ObjectInstanceHandle serving)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      entry.setServing(serving);
   }

   public synchronized void setServingName(int serial, String servingName)
         throws ArrayIndexOutOfBoundsException
   {
      Chef entry = _entries.elementAt(serial);
      entry.setServingName(servingName);
      entry.setServingNameState(AttributeState.OWNED_INCONSISTENT);
      fireTableRowsUpdated(serial, serial);
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
      Chef entry = _entries.elementAt(row);
      if (col == POSITION) return entry.getPosition().getAngle();
      else if (col == STATE) return entry.getState().getName();
      else if (col == SERVING) {
         if (entry.getState() == Chef.State.MAKING_SUSHI) return "";
         else return entry.getServing();
      }
      else if (col == BOAT) {
         if (entry.getState() == Chef.State.WAITING_TO_TRANSFER) return entry.getBoat();
         else return "";
      }
      else return null;
   }

   public synchronized boolean isCellEditable(int row, int col) { return false; }

   public synchronized void setValueAt(Object value, int row, int col) {
      // our table isn't editable from UI
   }

   public synchronized void updateChefs(
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
         Chef chef = _entries.elementAt(serial);
         boolean needToUpdate = false;
         AttributeHandleValueMap sa = attributeHandleValueMapFactory.create(3);
         if (chef.getPositionState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(positionAttribute, chef.getPosition().encode());
            needToUpdate = true;
            chef.setPositionState(AttributeState.OWNED_CONSISTENT);
         }
         if (chef.getServingNameState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(servingNameAttribute, InstanceName.encode(chef.getServingName()));
            needToUpdate = true;
            chef.setServingNameState(AttributeState.OWNED_CONSISTENT);
         }
         if (chef.getStateState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(stateAttribute, IntegerAttribute.encode(chef.getState().getOrdinal()));
            needToUpdate = true;
            chef.setStateState(AttributeState.OWNED_CONSISTENT);
         }
         if (needToUpdate) {
            rti.updateAttributeValues(chef.getHandle(), sa, null, sendTime);
         }
      }
   }
}
