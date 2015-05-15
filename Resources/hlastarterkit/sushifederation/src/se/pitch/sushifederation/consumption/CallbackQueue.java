//queue of RTI callbacks

package se.pitch.sushifederation.consumption;

import java.util.Vector;

public final class CallbackQueue {
  private Vector<Consumption.Callback> _list;

  public CallbackQueue() {
    _list = new Vector<Consumption.Callback>(10);
  }

  public synchronized void enqueue(Consumption.Callback callback) {
    _list.addElement(callback);
    notifyAll();
  }

  public synchronized Consumption.Callback dequeue() {
    Consumption.Callback callback;
    while (_list.size() == 0) {
      try {
        wait();
      } catch (InterruptedException e) {
         // ignore
      }
    }
    callback = (Consumption.Callback)_list.elementAt(0);
    _list.removeElementAt(0);
    return callback;
  }

  public synchronized int size() { return _list.size(); }
} 
