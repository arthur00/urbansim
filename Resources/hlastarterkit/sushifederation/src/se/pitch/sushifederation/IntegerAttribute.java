package se.pitch.sushifederation;

import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.RTIinternalError;

/** Attributes type and state are integers. Ensure uniform
 * representation across all federates.
 */
public final class IntegerAttribute {

   private static EncoderFactory _encoderFactory;

   static {
      try {
         _encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
      } catch (RTIinternalError rtIinternalError) {
         rtIinternalError.printStackTrace();
      }
   }

   /** Return new byte array with encoding of this instance.
    * Used to update attribute value.
    *
    * @param attribute : the attribute to encode
    *
    * @return : the attribute encoded as a byte array
    */
   public static byte[] encode(int attribute) {
      return _encoderFactory.createHLAinteger32BE(attribute).toByteArray();
   }


   /** Construct a new value from serialized version.
    * Used when reflecting an attribute value
    *
    * @param buffer : a byte buffer representing an integer attribute
    *
    * @return : the integer attribute
    *
    * @throws CouldNotDecode :
    */
   public static int decode(byte[] buffer)
         throws CouldNotDecode
   {
      try {
         HLAinteger32BE decoder = _encoderFactory.createHLAinteger32BE();
         decoder.decode(buffer);
         return decoder.getValue();
      } catch (Exception e) {
         throw new CouldNotDecode("IntegerAttribute.decode ("
               + new String(buffer, 0, buffer.length) + "): " + e.getMessage());
      }
   }
}

