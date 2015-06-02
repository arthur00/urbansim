package se.pitch.sushifederation.production;

import hla.rti1516e.LogicalTime;
import se.pitch.prti1516e.time.HLAfloat64TimeFactoryImpl;

import java.util.Vector;

public final class InternalQueue {
   private static String _newline = System.getProperty("line.separator");
   private Vector<Production.InternalEvent> _list; //all elements are Production.InternalEvents

   public InternalQueue() {
      _list = new Vector<Production.InternalEvent>(6);
   }

   public Production.InternalEvent dequeue() {
      if (_list.size() > 0) {
         Production.InternalEvent event = _list.elementAt(0);
         _list.removeElementAt(0);
         return event;
      }
      else return null;
   }

   public void enqueue(Production.InternalEvent event) {
      int size = _list.size();
      if (size == 0) {
         _list.addElement(event);
      }
      else {
         for (int index = 0; index < size; ++index) {
            if (event.getTime().compareTo(_list.elementAt(index).getTime()) < 0) {
               _list.insertElementAt(event, index);
               return;
            }
         }
         _list.addElement(event);
      }
   }

   public LogicalTime getTimeAtHead() {
      if (_list.size() > 0) {
         return _list.elementAt(0).getTime();
      }
      else {
         return new HLAfloat64TimeFactoryImpl().makeFinal();
      }
   }

   public String toString() {
      StringBuffer value = new StringBuffer("Queue:" + _newline);
      int size = _list.size();
      for (int index = 0; index < size; ++index) {
         Production.InternalEvent event = (Production.InternalEvent)_list.elementAt(index);
         value.append("  " + event.getTime());
         value.append(",  chef:" + event.getChef() + _newline);
      }
      return value.toString();
   }
}
