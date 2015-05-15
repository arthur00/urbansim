package se.pitch.sushifederation;


public enum AttributeState {
   OWNED_CONSISTENT(1),   //value unchanged since update
   OWNED_INCONSISTENT(2), //value changes since last update
   DISCOVERED(3),         //discovered but no data available
   REFLECTED(4),          //data available
   NOT_REFLECTED(5),      //not owned, not subscribed
   INVALID(0);

   private final int _ordinal;

   private AttributeState(int ordinal) {
      _ordinal = ordinal;
   }

   public int getOrdinal() {
      return _ordinal;
   }

   public static AttributeState find(int ordinal) {
      for (AttributeState a : values()) {
         if (a.getOrdinal() == ordinal) {
            return a;
         }
      }
      return INVALID;
   }
}
