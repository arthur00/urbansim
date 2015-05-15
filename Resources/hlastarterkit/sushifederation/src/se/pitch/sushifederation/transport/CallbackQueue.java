//queue of RTI callbacks

package se.pitch.sushifederation.transport;

import java.util.Vector;

public final class CallbackQueue {
  private Vector<Transport.Callback> _list;

  public CallbackQueue() {
    _list = new Vector<Transport.Callback>(10);
  }

  public synchronized void enqueue(Transport.Callback callback) {
    _list.addElement(callback);
    notifyAll();
  }

  public synchronized Transport.Callback dequeue() {
    Transport.Callback callback;
    while (_list.size() <= 0) {
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
