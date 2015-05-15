package se.pitch.sushifederation;

import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.*;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.RTIinternalError;

/** Representation of the position attribute. Defining the class ensures a uniform
 * representation across all federates.
 */
public final class Position implements Cloneable {

   private EncoderFactory _encoderFactory;

   private double _angle; //degrees counter-clockwise; zero is pos x axis (east)
   private OffsetEnum _offset;   //one of the constants below

   private Position() {
      try {
         RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
         _encoderFactory = rtiFactory.getEncoderFactory();
      } catch (RTIinternalError rtIinternalError) {
         rtIinternalError.printStackTrace();
      }
   }

   public Position(double angle, OffsetEnum offset) {
      this();
      _angle = angle;
      _offset = offset;
   }

   public Position(double angle, int offset) {
      this(angle, OffsetEnum.find(offset));
   }

   /** Construct a new instance from serialized version.
    * Used when reflecting an attribute value
    * @param buffer : a byte buffer that represents a position
    * @throws CouldNotDecode :
    */
   public Position(byte[] buffer) throws CouldNotDecode {
      this();

      final HLAfixedRecord positionDecoder = _encoderFactory.createHLAfixedRecord();
      try {
         HLAfloat64BE angleDecoder = _encoderFactory.createHLAfloat64BE();
         positionDecoder.add(angleDecoder);
         HLAinteger32BE offsetDecoder = _encoderFactory.createHLAinteger32BE();
         positionDecoder.add(offsetDecoder);
         positionDecoder.decode(buffer);

         _angle = angleDecoder.getValue();
         _offset = OffsetEnum.find(offsetDecoder.getValue());

      } catch (DecoderException e) {
         throw new CouldNotDecode("Position.decode("+ new String(buffer, 0, buffer.length) + "): " + e.getMessage());
      }
   }

   /** Return new byte array with encoding of this instance.
    * Used to update attribute value.
    * @return : the byte data that represents this position
    */
   public byte[] encode() {
      HLAfixedRecord pos = _encoderFactory.createHLAfixedRecord();
      pos.add(_encoderFactory.createHLAfloat64BE(_angle));
      pos.add(_encoderFactory.createHLAinteger32BE(_offset.getOffset()));
      return pos.toByteArray();
   }

   public double getAngle() {
      return _angle;
   }

   public OffsetEnum getOffset() {
      return _offset;
   }

   public String toString() {
      return "(" + Double.toString(_angle) + "," + _offset.getName() + ")";
   }

   public Object clone()
         throws CloneNotSupportedException
   {
      return super.clone();
   }

   public static void main(String[] args) throws CouldNotDecode {
      Position p = new Position(30, 1);
      byte[] b = p.encode();
      System.out.println(new Position(b));
   }
}

