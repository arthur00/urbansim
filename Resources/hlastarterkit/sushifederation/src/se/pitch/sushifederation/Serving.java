package se.pitch.sushifederation;

import hla.rti1516e.ObjectInstanceHandle;

public class Serving {
   private ObjectInstanceHandle _handle;
   private String _name;
   private AttributeState _privilegeToDeleteObjectState;
   private Position _position;
   private AttributeState _positionState;
   private int _type;
   private AttributeState _typeState;

   public ObjectInstanceHandle getHandle() {
      return _handle;
   }

   public void setHandle(ObjectInstanceHandle handle) {
      _handle = handle;
   }

   public String getName() {
      return _name;
   }

   public void setName(String name) {
      _name = name;
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

   public int getType() {
      return _type;
   }

   public void setType(int type) {
      _type = type;
   }

   public AttributeState getTypeState() {
      return _typeState;
   }

   public void setTypeState(AttributeState typeState) {
      _typeState = typeState;
   }
}