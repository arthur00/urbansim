package se.pitch.sushifederation;

import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.RTIinternalError;

/** The attributes cargo and servingName are
 * all names of object instances, so they are all represented as HLAunicodeStrings.
 */
public final class InstanceName {

   private static EncoderFactory _encoderFactory;

   static {
      try {
         _encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
      } catch (RTIinternalError rtIinternalError) {
         rtIinternalError.printStackTrace();
      }
   }

   /** Return new byte array with encoding of this instance.
    * Used to update attribute value or send a parameter.
    *
    * @param instanceName : the instance name to encode
    *
    * @return : the instance name encoded to a byte array
    */
   public static byte[] encode(String instanceName) {
      return _encoderFactory.createHLAunicodeString(instanceName).toByteArray();
   }


   /** Construct a new value from serialized version.
    * Used when reflecting an attribute value or receiving a parameter
    *
    * @param buffer : a byte buffer representing an instance name
    *
    * @return : the instance name as a String
    *
    * @throws  CouldNotDecode :
    */
   public static String decode(byte[] buffer) throws CouldNotDecode
   {
      try {
         HLAunicodeString name = _encoderFactory.createHLAunicodeString();
         name.decode(buffer);
         return name.getValue();
      } catch (Exception e) {
         throw new CouldNotDecode("InstanceName.decode: " + e.getMessage());
      }
   }
}
