package se.pitch.sushifederation.consumption;


public final class Barrier {
   boolean _set;
   Object[] _returnedValues;
   Object _suppliedValue;

   public Barrier() {
      _set = false;
      _suppliedValue = null;
   }

   public Barrier(Object suppliedValue) {
      _set = false;
      _suppliedValue = suppliedValue;
   }

   public synchronized void lower(Object[] returnedValues) {
      _returnedValues = returnedValues;
      _set = true;
      //awaken waiters
      notifyAll();
   }

   public synchronized Object[] await() {
      while (!_set) {
         try {
            wait();
         } catch (InterruptedException e) {
         }
      }
      _set = false;
      return _returnedValues;
   }

   public synchronized Object getSuppliedValue() {
    return _suppliedValue;
   }
}
