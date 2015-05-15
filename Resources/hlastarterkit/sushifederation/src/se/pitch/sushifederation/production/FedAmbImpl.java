package se.pitch.sushifederation.production;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;

/**
 * Production's implementation of FederateAmbassador
 */
public final class FedAmbImpl extends NullFederateAmbassador {

   private ProductionFrame _userInterface;
   private Production _fed;
   private Barrier _enableTimeConstrainedBarrier = null;
   private Barrier _enableTimeRegulationBarrier = null;
   private Barrier _synchronizationPointRegistrationSucceededBarrier = null;
   private Barrier _federationSynchronizedBarrier = null;


   public void setEnableTimeConstrainedBarrier(Barrier b) {
      _enableTimeConstrainedBarrier = b;
   }

   public void setEnableTimeRegulationBarrier(Barrier b) {
      _enableTimeRegulationBarrier = b;
   }

   public void setFederationSynchronizedBarrier(Barrier b) {
      _federationSynchronizedBarrier = b;
   }

   public void setSynchronizationPointRegistrationSucceededBarrier(Barrier b) {
      _synchronizationPointRegistrationSucceededBarrier = b;
   }

   public FedAmbImpl(
         Production fed,
         ProductionFrame ui)
   {
      _fed = fed;
      _userInterface = ui;
   }

   @Override
   public void objectInstanceNameReservationSucceeded(String objectName) throws FederateInternalError {
      _fed.objectInstanceNameReservationSucceeded();
   }

   @Override
   public void objectInstanceNameReservationFailed(String objectName) throws FederateInternalError {
      _fed.objectInstanceNameReservationFailed();
   }

   //4.8

   @Override
   public void announceSynchronizationPoint(
         String synchronizationPointLabel,
         byte[] userSuppliedTag)
         throws
         FederateInternalError
   {
      _fed.recordSynchronizationPointAnnouncement(synchronizationPointLabel);
   }

   // 7.16

   @Override
   public void attributeIsNotOwned(ObjectInstanceHandle theObject,
                                   AttributeHandle theAttribute) throws FederateInternalError
   {
      _userInterface.post("�(7.16)Attribute is not owned, object: " + theObject
            + ", attribute: " + theAttribute);
   }

   // 7.16

   @Override
   public void attributeIsOwnedByRTI(ObjectInstanceHandle theObject,
                                     AttributeHandle theAttribute) throws FederateInternalError
   {
      _userInterface.post("�(7.16)Attribute owned by RTI, object: " + theObject
            + ", attribute: " + theAttribute);
   }

   // 7.6

   @Override
   public void attributeOwnershipAcquisitionNotification(ObjectInstanceHandle theObject,
                                                         AttributeHandleSet securedAttributes,
                                                         byte[] userSuppliedTag) throws FederateInternalError
   {
      _userInterface.post("�(7.6)attributeOwnershipAcquisitionNotification; object:"
            + theObject
            + ", attrs: " + securedAttributes);
   }

   // 7.5

   @Override
   public void requestDivestitureConfirmation(ObjectInstanceHandle theObject,
                                              AttributeHandleSet offeredAttributes) throws FederateInternalError
   {
      _fed.queueAttributeOwnershipDivestitureNotification(theObject);
   }

   // 7.9

   @Override
   public void attributeOwnershipUnavailable(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes) throws FederateInternalError {
       _userInterface.post("�(7.9)attributeOwnershipUnavailable; object:"
            + theObject
            + ", attrs: " + theAttributes);
   }

   // 6.13

   @Override
   public void attributesInScope(ObjectInstanceHandle theObject,
                                 AttributeHandleSet theAttributes) throws FederateInternalError
   {
      _userInterface.post("�(6.15)attributesInScope; object:"
            + theObject
            + ", attrs: " + theAttributes);
   }

   // 6.14

   @Override
   public void attributesOutOfScope(ObjectInstanceHandle theObject,
                                    AttributeHandleSet theAttributes) throws FederateInternalError
   {
      _userInterface.post("�(6.16)attributesOutOfScope; object:"
            + theObject
            + ", attrs: " + theAttributes);
   }

   // 7.14

   @Override
   public void confirmAttributeOwnershipAcquisitionCancellation(ObjectInstanceHandle theObject,
                                                                AttributeHandleSet theAttributes) throws FederateInternalError
   {
      _userInterface.post("�(7.14)confirmAttributeOwnershipAcquisitionCancellation; object:"
            + theObject
            + ", attrs: " + theAttributes);
   }

   // 6.5

   @Override
   public void discoverObjectInstance(ObjectInstanceHandle theObject,
                                      ObjectClassHandle theObjectClass,
                                      String objectName,
                                      FederateHandle producingFederate) throws FederateInternalError
   {
      _fed.queueDiscoverObjectInstance(theObject, theObjectClass, objectName);
   }

   @Override
   public void discoverObjectInstance(ObjectInstanceHandle theObject,
                                      ObjectClassHandle theObjectClass,
                                      String objectName) throws FederateInternalError
   {
      _fed.queueDiscoverObjectInstance(theObject, theObjectClass, objectName);
   }

   // 4.21

   @Override
   public void federationNotRestored(RestoreFailureReason reason) throws FederateInternalError
   {
      _userInterface.post("�(4.21)Federation not restored.");
   }

   // 4.15

   @Override
   public void federationNotSaved(SaveFailureReason reason) throws FederateInternalError
   {
      _userInterface.post("�(4.15)Federation not saved, reason: " + reason);
   }

   // 4.18

   @Override
   public void federationRestoreBegun ()
         throws
         FederateInternalError
   {
      _userInterface.post("�(4.18)Federation restore begun.");
   }

   // 4.21

   @Override
   public void federationRestored ()
         throws
         FederateInternalError
   {
      _userInterface.post("�(4.21)Federation restored.");
   }

   // 4.15

   @Override
   public void federationSaved ()
         throws
         FederateInternalError
   {
      _userInterface.post("�(4.15)Federation saved.");
   }
   
   //4.10

   @Override
   public void federationSynchronized(String synchronizationPointLabel,
                                      FederateHandleSet failedToSyncSet) throws FederateInternalError
   {
      if (_federationSynchronizedBarrier != null) {
         if (_federationSynchronizedBarrier.getSuppliedValue().equals(synchronizationPointLabel)) {
            _federationSynchronizedBarrier.lower(null);
         }
         else {
            _userInterface.post("�(4.10)federationSynchronized at:"
                  + synchronizationPointLabel);
         }
      }
      else {
         _userInterface.post("ERROR: federationSynchronized with no barrier set");
      }
   }

   // 7.16

   @Override
   public void informAttributeOwnership(ObjectInstanceHandle theObject,
                                        AttributeHandle theAttribute,
                                        FederateHandle theOwner) throws FederateInternalError
   {
      _userInterface.post("�(7.16)informAttributeOwnership; object:"
            + theObject
            + ", attr: " + theAttribute
            + ", owner: " + theOwner);
   }

   // 4.19

   @Override
   public void initiateFederateRestore(String label,
                                       String federateName,
                                       FederateHandle federateHandle) throws FederateInternalError
   {
      _userInterface.post("�(4.19)Initiate federate restore.");
   }

   //4.12

   @Override
   public void initiateFederateSave(String label) throws FederateInternalError {
       _userInterface.post("�(4.12)Initiate federate save.");
   }

   @Override
   public void initiateFederateSave(String label, LogicalTime time) throws FederateInternalError
   {
      _userInterface.post("�(4.12)Initiate federate save.");
   }

   // 6.18

   @Override
   public void provideAttributeValueUpdate(ObjectInstanceHandle theObject,
                                           AttributeHandleSet theAttributes,
                                           byte[] userSuppliedTag) throws FederateInternalError
   {
      _fed.queueProvideAttributeValueUpdateCallback(theObject, theAttributes);
   }

   // 6.9

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass,
                                  ParameterHandleValueMap theParameters,
                                  byte[] userSuppliedTag,
                                  OrderType sentOrdering,
                                  TransportationTypeHandle theTransport,
                                  SupplementalReceiveInfo receiveInfo) throws FederateInternalError
   {
      _fed.queueReceiveInteractionCallback(interactionClass);
   }

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass,
                                  ParameterHandleValueMap theParameters,
                                  byte[] userSuppliedTag,
                                  OrderType sentOrdering,
                                  TransportationTypeHandle theTransport,
                                  LogicalTime theTime,
                                  OrderType receivedOrdering,
                                  MessageRetractionHandle retractionHandle,
                                  SupplementalReceiveInfo receiveInfo) throws FederateInternalError
   {
      _fed.queueReceiveInteractionEvent(interactionClass, theParameters, theTime);
   }

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass,
                                  ParameterHandleValueMap theParameters,
                                  byte[] userSuppliedTag,
                                  OrderType sentOrdering,
                                  TransportationTypeHandle theTransport,
                                  LogicalTime theTime,
                                  OrderType receivedOrdering,
                                  SupplementalReceiveInfo receiveInfo) throws FederateInternalError
   {
      _fed.queueReceiveInteractionEvent(interactionClass, theParameters, theTime);
   }

   // 6.7

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                      AttributeHandleValueMap theAttributes,
                                      byte[] userSuppliedTag,
                                      OrderType sentOrdering,
                                      TransportationTypeHandle theTransport,
                                      SupplementalReflectInfo reflectInfo) throws FederateInternalError
   {
      _userInterface.post("�(6.7)reflectAttributeValues of obj " + theObject
            + ", tag: " + renderTag(userSuppliedTag));
      for (AttributeHandle handle : theAttributes.keySet()) {
         _userInterface.post("  attr: " + handle
               + " value: " + new String(theAttributes.get(handle)));
      }
   }

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                      AttributeHandleValueMap theAttributes,
                                      byte[] userSuppliedTag,
                                      OrderType sentOrdering,
                                      TransportationTypeHandle theTransport,
                                      LogicalTime theTime,
                                      OrderType receivedOrdering,
                                      SupplementalReflectInfo reflectInfo) throws FederateInternalError
   {
      _fed.queueReflectAttributeValuesEvent(
            theTime,
            theObject,
            theAttributes,
            userSuppliedTag);
   }

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                      AttributeHandleValueMap theAttributes,
                                      byte[] userSuppliedTag,
                                      OrderType sentOrdering,
                                      TransportationTypeHandle theTransport,
                                      LogicalTime theTime,
                                      OrderType receivedOrdering,
                                      MessageRetractionHandle retractionHandle,
                                      SupplementalReflectInfo reflectInfo) throws FederateInternalError
   {
      _fed.queueReflectAttributeValuesEvent(
            theTime,
            theObject,
            theAttributes,
            userSuppliedTag);
   }

   // 6.11

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject,
                                    byte[] userSuppliedTag,
                                    OrderType sentOrdering,
                                    SupplementalRemoveInfo removeInfo) throws FederateInternalError
   {
      _userInterface.post("�(6.11)removeObjectInstance; object:"
            + theObject + ", tag: " + renderTag(userSuppliedTag));
   }

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject,
                                    byte[] userSuppliedTag,
                                    OrderType sentOrdering,
                                    LogicalTime theTime,
                                    OrderType receivedOrdering,
                                    SupplementalRemoveInfo removeInfo) throws FederateInternalError
   {
      _fed.queueRemoveObjectInstanceCallback(theObject);
   }

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject,
                                    byte[] userSuppliedTag,
                                    OrderType sentOrdering,
                                    LogicalTime theTime,
                                    OrderType receivedOrdering,
                                    MessageRetractionHandle retractionHandle,
                                    SupplementalRemoveInfo removeInfo) throws FederateInternalError
   {
      _fed.queueRemoveObjectInstanceCallback(theObject);
   }

   // 7.4

   @Override
   public void requestAttributeOwnershipAssumption(ObjectInstanceHandle theObject,
                                                   AttributeHandleSet offeredAttributes,
                                                   byte[] userSuppliedTag) throws FederateInternalError
   {
      /*
    _userInterface.post("�(7.4)requestAttributeOwnershipAssumption; object:"
			+ theObject
      + ", attrs: " + offeredAttributes
      + ", tag: " + renderTag(userSuppliedTag));
      */
   }

   // 7.10


   @Override
   public void requestAttributeOwnershipRelease(ObjectInstanceHandle theObject,
                                                AttributeHandleSet candidateAttributes,
                                                byte[] userSuppliedTag) throws FederateInternalError
   {
      _fed.queueRequestAttributeOwnershipReleaseCallback(
            theObject,
            candidateAttributes,
            userSuppliedTag);
   }

   // 4.17

   @Override
   public void requestFederationRestoreFailed(String label) throws FederateInternalError
   {
      _userInterface.post("�(4.17)Request federation restore failed, label: " + label);
   }

   // 4.17

   @Override
   public void requestFederationRestoreSucceeded (
         String label)
         throws
         FederateInternalError
   {
      _userInterface.post("�(4.17)Request federation restore succeeded, label: "
            + label);
   }

   // 8.22

   @Override
   public void requestRetraction(MessageRetractionHandle theHandle) throws FederateInternalError
   {
      _userInterface.post("�(8.22)Request retraction, handle: " + theHandle);
   }

   // 5.10

   @Override
   public void startRegistrationForObjectClass(ObjectClassHandle theClass) throws FederateInternalError
   {
      _userInterface.post("�(5.10)startRegistrationForObjectClass: " + theClass);
   }

   // 5.11


   @Override
   public void stopRegistrationForObjectClass(ObjectClassHandle theClass) throws FederateInternalError
   {
      _userInterface.post("�(5.11)stopRegistrationForObjectClass:"+ theClass);
   }

   //4.7

   @Override
   public void synchronizationPointRegistrationFailed(String synchronizationPointLabel,
                                                      SynchronizationPointFailureReason reason) throws FederateInternalError
   {
      _userInterface.post("�(4.7)synchronizationPointRegistrationFailed; label: " + synchronizationPointLabel);
   }

   //4.7

   @Override
   public void synchronizationPointRegistrationSucceeded(
         String synchronizationPointLabel)
         throws
         FederateInternalError
   {
      if (_synchronizationPointRegistrationSucceededBarrier != null) {
         if (_synchronizationPointRegistrationSucceededBarrier.getSuppliedValue().equals(synchronizationPointLabel)) {
            _synchronizationPointRegistrationSucceededBarrier.lower(null);
         }
         else {
            _userInterface.post("�(4.7)synchronizationPointRegistrationSucceeded; label: "
                  + synchronizationPointLabel);
         }
      }
      else {
         _userInterface.post("ERROR: synchronizationPointRegistrationSucceeded with no barrier set");
      }
   }

   // 8.13

   @Override
   public void timeAdvanceGrant (LogicalTime theTime) throws FederateInternalError
   {
      _fed.queueGrantEvent(theTime);
   }

   // 8.6

   @Override
   public void timeConstrainedEnabled (LogicalTime theFederateTime) throws FederateInternalError
   {
      if (_enableTimeConstrainedBarrier != null) {
         Object[] returnedTime = {theFederateTime};
         _enableTimeConstrainedBarrier.lower(returnedTime);
      }
      else {
         _userInterface.post("ERROR: timeConstrainedEnabled with no barrier set");
      }
   }

   // 8.3

   @Override
   public void timeRegulationEnabled (LogicalTime theFederateTime) throws FederateInternalError
   {
      if (_enableTimeRegulationBarrier != null) {
         Object[] returnedTime = {theFederateTime};
         _enableTimeRegulationBarrier.lower(returnedTime);
      }
      else {
         _userInterface.post("ERROR: timeRegulationEnabled with no barrier set");
      }
   }

   // 5.13

   @Override
   public void turnInteractionsOff(InteractionClassHandle theHandle) throws FederateInternalError
   {
      _userInterface.post("�(5.13)turnInteractionsOff: " + theHandle);
   }

   // 5.12

   @Override
   public void turnInteractionsOn(InteractionClassHandle theHandle) throws FederateInternalError
   {
      _userInterface.post("�(5.12)turnInteractionsOn: " + theHandle);
   }

   // 6.20

   @Override
   public void turnUpdatesOffForObjectInstance(ObjectInstanceHandle theObject,
                                               AttributeHandleSet theAttributes) throws FederateInternalError
   {
      _userInterface.post("�(6.20)Turn updates off for object instance: "
            + theObject + ", attributes: " + theAttributes);
   }

   // 6.19

   @Override
   public void turnUpdatesOnForObjectInstance(ObjectInstanceHandle theObject,
                                              AttributeHandleSet theAttributes) throws FederateInternalError {
      _userInterface.post("�(6.19)Turn updates on for object instance: "
            + theObject + ", attributes: " + theAttributes);
   }

   @Override
   public void turnUpdatesOnForObjectInstance(ObjectInstanceHandle theObject,
                                              AttributeHandleSet theAttributes, String updateRateDesignator) throws FederateInternalError
   {
      _userInterface.post("�(6.19)Turn updates on for object instance: "
            + theObject + ", attributes: " + theAttributes);
   }

   private String renderTag(byte[] tag) {
      return (tag == null) ? "[null]" : new String(tag);
   }
}
