package se.pitch.sushifederation;

import hla.rti1516e.ObjectInstanceHandle;

public class Diner {

   //legal values for _state
   public static enum State {
      INVALID("Invalid", 0),
      LOOKING_FOR_FOOD("Looking for food", 1),
      ACQUIRING("Acquiring", 2),
      EATING("Eating", 3),
      PREPARING_TO_DELETE_SERVING("Preparing to delete serving", 4);

      private final String _name;
      private final int _ordinal;

      private State(String name, int ordinal) {
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
         for (State state : State.values()) {
            if (state.getOrdinal() == ordinal) {
               return state;
            }
         }
         return INVALID;
      }
   }

   //attribute values
   private AttributeState _privilegeToDeleteObjectState;
   private Position _position;
   private AttributeState _positionState;
   private State _state;
   private AttributeState _stateState;
   private String _servingName;
   private AttributeState _servingNameState;

   //internal
   private ObjectInstanceHandle _handle;
   private String _name;
   private ObjectInstanceHandle _serving; //handle for _servingName
   private ObjectInstanceHandle _boat; //handle of Boat we're trying to transfer from

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

   public AttributeState getStateState() {
      return _stateState;
   }

   public void setStateState(AttributeState stateState) {
      _stateState = stateState;
   }

   public String getServingName() {
      return _servingName;
   }

   public void setServingName(String servingName) {
      _servingName = servingName;
   }

   public AttributeState getServingNameState() {
      return _servingNameState;
   }

   public void setServingNameState(AttributeState servingNameState) {
      _servingNameState = servingNameState;
   }

   public String getName() {
      return _name;
   }

   public void setName(String name) {
      _name = name;
   }

   public ObjectInstanceHandle getHandle() {
      return _handle;
   }

   public void setHandle(ObjectInstanceHandle handle) {
      _handle = handle;
   }

   public State getState() {
      return _state;
   }

   public void setState(State state) {
      _state = state;
   }

   public ObjectInstanceHandle getServing() {
      return _serving;
   }

   public void setServing(ObjectInstanceHandle serving) {
      _serving = serving;
   }

   public ObjectInstanceHandle getBoat() {
      return _boat;
   }

   public void setBoat(ObjectInstanceHandle boat) {
      _boat = boat;
   }
}

