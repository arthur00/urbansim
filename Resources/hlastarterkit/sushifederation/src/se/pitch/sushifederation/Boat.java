package se.pitch.sushifederation;

import hla.rti1516e.ObjectInstanceHandle;

public class Boat {

   public enum State {
      INVALID("Invalid", 0),
      EMPTY("Empty", 1),
      AWAITING_SERVING("Awaiting Serving", 2),
      LOADED("Loaded", 3);

      private String _name;
      private int _ordinal;

      State(String name, int ordinal) {
         _name = name;
         _ordinal = ordinal;
      }

      public String getName() {
         return _name;
      }

      public int getOrdinal() {
         return _ordinal;
      }

      public static State find(int ordinal) {
         for (State s : values()) {
            if (s.getOrdinal() == ordinal) {
               return s;
            }
         }
         return INVALID;
      }
   }

   //attribute values
   private AttributeState _privilegeToDeleteObjectState;
   private Position _position;
   private AttributeState _positionState;
   private boolean _spaceAvailable; //true iff _modelingState == EMPTY
   private AttributeState _spaceAvailableState;
   private String _cargo; //name of a Serving instance
   private AttributeState _cargoState;

   //internal
   private ObjectInstanceHandle _handle;
   private String _name;
   private State _modelingState;
   private ObjectInstanceHandle _serving; //Serving handle corresponding to _cargo

   public ObjectInstanceHandle getHandle() {
      return _handle;
   }

   public void setHandle(ObjectInstanceHandle handle) {
      _handle = handle;
   }

   public ObjectInstanceHandle getServing() {
      return _serving;
   }

   public void setServing(ObjectInstanceHandle serving) {
      _serving = serving;
   }

   public AttributeState getPrivilegeToDeleteObjectState() {
      return _privilegeToDeleteObjectState;
   }

   public void setPrivilegeToDeleteObjectState(AttributeState privilegeToDeleteObjectState) {
      _privilegeToDeleteObjectState = privilegeToDeleteObjectState;
   }

   public Position getPosition() {
      return _position;
   }

   public void setPosition(Position position) {
      _position = position;
   }

   public AttributeState getPositionState() {
      return _positionState;
   }

   public void setPositionState(AttributeState positionState) {
      _positionState = positionState;
   }

   public boolean isSpaceAvailable() {
      return _spaceAvailable;
   }

   public void setSpaceAvailable(boolean spaceAvailable) {
      _spaceAvailable = spaceAvailable;
   }

   public AttributeState getSpaceAvailableState() {
      return _spaceAvailableState;
   }

   public void setSpaceAvailableState(AttributeState spaceAvailableState) {
      _spaceAvailableState = spaceAvailableState;
   }

   public String getCargo() {
      return _cargo;
   }

   public void setCargo(String cargo) {
      _cargo = cargo;
   }

   public AttributeState getCargoState() {
      return _cargoState;
   }

   public void setCargoState(AttributeState cargoState) {
      _cargoState = cargoState;
   }

   public String getName() {
      return _name;
   }

   public void setName(String name) {
      _name = name;
   }

   public State getModelingState() {
      return _modelingState;
   }

   public void setModelingState(State state) {
      _modelingState = state;
   }
}
