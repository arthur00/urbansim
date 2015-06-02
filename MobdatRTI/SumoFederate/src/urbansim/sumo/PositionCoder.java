package urbansim.sumo;
import hla.rti1516e.encoding.*;

class Position {

	   private final double _x;
	   private final double _y;
	   private final double _z;

	   public Position(double x, double y, double z) {
	      _x = x;
	      _y = y;
	      _z = z;
	   }

	   public double getX() {
	      return _x;
	   }

	   public double getY() {
	      return _y;
	   }
	   
	   public double getZ() {
		  return _z;
	   }
	   
	   @Override
	   public String toString() {
	      return "Position[" + _x + ", " + _y + ", " + _z + "]";
	   }
}

class PositionCoder {

   private final HLAfixedRecord _coder;
   private final HLAfloat64BE _x;
   private final HLAfloat64BE _y;
   private final HLAfloat64BE _z;

   PositionCoder(EncoderFactory encoderFactory) {
      _coder = encoderFactory.createHLAfixedRecord();
      _x = encoderFactory.createHLAfloat64BE();
      _coder.add(_x);
      _y = encoderFactory.createHLAfloat64BE();
      _coder.add(_y);
      _z = encoderFactory.createHLAfloat64BE();
      _coder.add(_z);
   }

   Position decode(byte[] bytes) throws DecoderException {
      _coder.decode(bytes);
      return new Position(_x.getValue(), _y.getValue(), _z.getValue());
   }

   byte[] encode(Position position) {
      _x.setValue(position.getX());
      _y.setValue(position.getY());
      _z.setValue(position.getZ());
      return _coder.toByteArray();
   }
}
