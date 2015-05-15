package se.pitch.sushifederation;

import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.RTIinternalError;


/** The type attribute is a boolean. Defining the class ensures a uniform
 * representation across all federates.
 */
public final class SpaceAvailable {

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
    * @param spaceAvailable : the boolean to be encoded
    *
    * @return : the byte array encoding of the boolean
    */
   public static byte[] encode(boolean spaceAvailable) {
      final HLAboolean encoder = _encoderFactory.createHLAboolean(spaceAvailable);
      return encoder.toByteArray();
   }


   /** Construct a new value from serialized version.
    * Used when reflecting an attribute value
    *
    * @param buffer : a byte array representing a boolean
    *
    * @return : the decoded boolean
    *
    * @throws CouldNotDecode :
    */
   public static boolean decode(byte[] buffer)
         throws CouldNotDecode
   {
      final HLAboolean decoder = _encoderFactory.createHLAboolean();
      try {
         decoder.decode(buffer);
      } catch (DecoderException e) {
         throw new CouldNotDecode("SpaceAvailable.decode ("
               + new String(buffer, 0, buffer.length) + "): " + e.getMessage());
      }
      return decoder.getValue();
   }
}

