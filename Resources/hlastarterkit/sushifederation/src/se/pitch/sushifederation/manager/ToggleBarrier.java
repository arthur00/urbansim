package se.pitch.sushifederation.manager;


public final class ToggleBarrier {
   boolean _lowered;
   Object _returnedValue;

   public ToggleBarrier() {
      _lowered = true;
      _returnedValue = null;
   }

   public synchronized void raise() {
      _lowered = false;
   }

   public synchronized void lower() {
      _lowered = true;
      _returnedValue = null;
      //awaken waiters
      notifyAll();
   }

   public synchronized void lower(Object returnedValue) {
      _lowered = true;
      _returnedValue = returnedValue;
      //awaken waiters
      notifyAll();
   }

   public synchronized Object await() {
      while (!_lowered) {
         try {
            wait();
         } catch (InterruptedException e) {
         }
      }
      return _returnedValue;
   }
}
