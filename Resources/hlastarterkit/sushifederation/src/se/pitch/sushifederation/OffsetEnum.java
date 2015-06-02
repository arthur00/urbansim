package se.pitch.sushifederation;

/**
 * Created by IntelliJ IDEA.
 * User: asa
 * Date: 5/19/11
 * Time: 2:31 PM
 */
public enum OffsetEnum {
   INBOARD_CANAL("Inboard canal", 1),
   ON_CANAL("On canal", 2),
   OUTBOARD_CANAL("Outboard Canal", 3),
   INVALID("Invalid", 0);

   private String _name;
   private int _offset;

   private OffsetEnum(String name, int offset) {
      _name = name;
      _offset = offset;
   }

   public int getOffset() {
      return _offset;
   }

   public String getName() {
      return _name;
   }

   public static OffsetEnum find(int offset) {
      for (OffsetEnum e : values()) {
         if (e.getOffset() == offset) {
            return e;
         }
      }
      return INVALID;
   }
}
