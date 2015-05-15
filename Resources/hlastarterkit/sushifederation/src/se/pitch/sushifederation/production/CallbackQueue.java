package se.pitch.sushifederation.production;

import java.util.Vector;

/**
 * queue of RTI callbacks
 */
public final class CallbackQueue {
  private Vector<Production.Callback> _list;

  public CallbackQueue() {
    _list = new Vector<Production.Callback>(10);
  }

  public synchronized void enqueue(Production.Callback event) {
    _list.addElement(event);
    notifyAll();
  }

  public synchronized Production.Callback dequeue() {
    Production.Callback event;
    while (_list.size() == 0) {
      try {
        wait();
      } catch (InterruptedException e) {
         // ignore
      }
    }
    event = (Production.Callback)_list.elementAt(0);
    _list.removeElementAt(0);
    return event;
  }
}
