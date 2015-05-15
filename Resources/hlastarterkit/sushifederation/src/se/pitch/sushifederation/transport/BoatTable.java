//holds instance attribute state for the federate
//used as the 'model' for the UI view (JTable)

package se.pitch.sushifederation.transport;

import hla.rti1516e.ObjectInstanceHandle;
import se.pitch.sushifederation.AttributeState;
import se.pitch.sushifederation.Boat;
import se.pitch.sushifederation.OffsetEnum;
import se.pitch.sushifederation.Position;

import javax.swing.table.AbstractTableModel;
import java.util.Hashtable;
import java.util.Vector;

public final class BoatTable extends AbstractTableModel
{
  //columns for this table
  public final static int HANDLE = 0;
  public final static int NAME = 1;
  public final static int POSITION = 2;
  public final static int STATE = 3;
  public final static int SERVING = 4;
  public final static int COL_COUNT = 5;

  //entry: Boat
  private Vector<Boat> _entries = new Vector<Boat>();
  //key: handle value: serial (row in table). Both as Integers
  private Hashtable<ObjectInstanceHandle, Integer> _boatsByHandle = new Hashtable<ObjectInstanceHandle, Integer>();
  private String[] _columnNames = {
    "Handle",
    "Name",
    "Position",
    "State",
    "Serving"};

  public int getSerialByHandle(ObjectInstanceHandle handle)
  throws ArrayIndexOutOfBoundsException
  {
    Integer serial = _boatsByHandle.get(handle);
    if (serial == null) throw new ArrayIndexOutOfBoundsException("No Boat for handle "
      + handle);
    return serial;
  }

  public synchronized ObjectInstanceHandle getHandleBySerial(int serial)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    return entry.getHandle();
  }

  public synchronized String getNameBySerial(int serial)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    return entry.getName();
  }

  public synchronized double getPositionBySerial(int serial)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    return entry.getPosition().getAngle();
  }

  public synchronized double getPosition(ObjectInstanceHandle handle)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(getSerialByHandle(handle));
    return entry.getPosition().getAngle();
  }

  public synchronized Boat.State getState(ObjectInstanceHandle handle)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(getSerialByHandle(handle));
    return entry.getModelingState();
  }

  public synchronized Boat.State getStateBySerial(int serial)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    return entry.getModelingState();
  }

  public synchronized ObjectInstanceHandle getServing(ObjectInstanceHandle handle)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(getSerialByHandle(handle));
    return entry.getServing();
  }

  public synchronized ObjectInstanceHandle getServingBySerial(int serial)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    return entry.getServing();
  }

  //add new Boat
  public synchronized void add(
    ObjectInstanceHandle handle,
    String name,
    double position,
    Boat.State state,
    ObjectInstanceHandle serving,
    String servingName)
  {
    Boat newEntry = new Boat();
    newEntry.setHandle(handle);
    newEntry.setName(name);
    newEntry.setPosition(new Position(position, OffsetEnum.ON_CANAL));
    newEntry.setPositionState(AttributeState.OWNED_INCONSISTENT);
    newEntry.setModelingState(state);
    newEntry.setServing(serving);
    newEntry.setCargo(servingName);
    newEntry.setCargoState(AttributeState.OWNED_INCONSISTENT);
    newEntry.setSpaceAvailable(true);
    newEntry.setSpaceAvailableState(AttributeState.OWNED_INCONSISTENT);
    newEntry.setPrivilegeToDeleteObjectState(AttributeState.OWNED_INCONSISTENT);
    _entries.addElement(newEntry);
    //update serial-by-handle table
    int index = _entries.size() - 1;
    _boatsByHandle.put(handle, index);
    //tell TableView to update
    fireTableRowsInserted(index, index);
    fireTableRowsUpdated(index, index);
  }

  //return the Boat row that carries a given serving
  //-1 if not found
  public synchronized int getBoatForServing(ObjectInstanceHandle servingHandle) {
    int size = _entries.size();
    for (int i = 0; i < size; ++i) {
      Boat entry = _entries.elementAt(i);
      if (entry.getServing() == servingHandle) return i;
    }
    return -1;
  }

  public synchronized void setState(ObjectInstanceHandle handle, Boat.State state)
  throws ArrayIndexOutOfBoundsException
  {
    int serial = getSerialByHandle(handle);
    Boat entry = _entries.elementAt(serial);
    entry.setModelingState(state);
    fireTableCellUpdated(serial, STATE);
  }

  public synchronized void setCargoBySerial(int serial, String servingName)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    entry.setCargo(servingName);
    fireTableCellUpdated(serial, SERVING);
  }

  public synchronized void setSpaceAvailableBySerial(int serial, boolean flag)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    entry.setSpaceAvailable(flag);
  }

  public synchronized void setPositionBySerial(int serial, double position)
  throws ArrayIndexOutOfBoundsException
  {
    Boat entry = _entries.elementAt(serial);
    entry.setPosition(new Position(position, OffsetEnum.ON_CANAL));
    fireTableCellUpdated(serial, POSITION);
  }

  public synchronized void setServing(ObjectInstanceHandle handle, ObjectInstanceHandle serving)
  throws ArrayIndexOutOfBoundsException
  {
    int serial = getSerialByHandle(handle);
    Boat entry = _entries.elementAt(serial);
    entry.setServing(serving);
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
    Boat entry = _entries.elementAt(row);
    if (col == HANDLE) return entry.getHandle();
    else if (col == NAME) return entry.getName();
    else if (col == POSITION) return entry.getPosition().getAngle();
    else if (col == STATE) return entry.getModelingState().getName();
    else if (col == SERVING) return entry.getCargo();
    else return null;
  }

  public synchronized boolean isCellEditable(int row, int col) { return false; }

  public synchronized void setValueAt(Object value, int row, int col) {
    // our table isn't editable from UI
  }
}
