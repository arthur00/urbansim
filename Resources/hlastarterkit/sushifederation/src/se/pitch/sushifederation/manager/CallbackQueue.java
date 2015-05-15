package se.pitch.sushifederation.manager;

import java.util.Vector;

public final class CallbackQueue {
  private Vector _list;

  public CallbackQueue() {
    _list = new Vector(10);
  }

  public synchronized void enqueue(Manager.Callback event) {
    _list.addElement(event);
    notifyAll();
  }

  public synchronized Manager.Callback dequeue() {
    Manager.Callback event;
    while (_list.size() == 0) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
    event = (Manager.Callback)_list.elementAt(0);
    _list.removeElementAt(0);
    return event;
  }
} 
