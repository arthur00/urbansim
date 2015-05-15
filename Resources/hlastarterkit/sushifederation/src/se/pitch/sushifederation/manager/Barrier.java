package se.pitch.sushifederation.manager;


public final class Barrier {
   boolean _lowered;
   Object[] _returnedValues;
   Object _suppliedValue;

   public Barrier() {
      _lowered = false;
      _suppliedValue = null;
   }

   public Barrier(Object suppliedValue) {
      _lowered = false;
      _suppliedValue = suppliedValue;
   }

   public synchronized void lower(Object[] returnedValues) {
      _returnedValues = returnedValues;
      _lowered = true;
      //awaken waiters
      notifyAll();
   }

   public synchronized Object[] await() {
      while (!_lowered) {
         try {
            wait();
         } catch (InterruptedException e) {
         }
      }
      _lowered = false;  //semantics are one-shot
      return _returnedValues;
   }

   public synchronized Object getSuppliedValue() {
    return _suppliedValue;
   }
}
