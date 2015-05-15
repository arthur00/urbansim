//queue of RTI callbacks

package se.pitch.sushifederation.viewer;

import java.util.Vector;

public final class CallbackQueue {
  private Vector<Viewer.Callback> _list;

  public CallbackQueue() {
    _list = new Vector<Viewer.Callback>(10);
  }

  public synchronized void enqueue(Viewer.Callback callback) {
    _list.addElement(callback);
    notifyAll();
  }

  public synchronized Viewer.Callback dequeue() {
    Viewer.Callback callback;
    while (_list.size() == 0) {
      try {
        wait();
      } catch (InterruptedException e) {
         // ignore
      }
    }
    callback = _list.elementAt(0);
    _list.removeElementAt(0);
    return callback;
  }
} 
