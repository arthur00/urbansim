package se.pitch.sushifederation;

public class RestaurantNames {

  //object class names
  public static final String _RestaurantClassName = "HLAobjectRoot.Restaurant";
  public static final String _ServingClassName = "HLAobjectRoot.Restaurant.Serving";
  public static final String _BoatClassName = "HLAobjectRoot.Restaurant.Boat";
  public static final String _ActorClassName = "HLAobjectRoot.Restaurant.Actor";
  public static final String _ChefClassName = "HLAobjectRoot.Restaurant.Actor.Chef";
  public static final String _DinerClassName = "HLAobjectRoot.Restaurant.Actor.Diner";

  //attribute names
  public static final String _privilegeToDeleteObjectAttributeName = "privilegeToDeleteObject";
  public static final String _positionAttributeName = "Position";
  public static final String _typeAttributeName = "Type";
  public static final String _spaceAvailableAttributeName = "SpaceAvailable";
  public static final String _cargoAttributeName = "Cargo";
  public static final String _chefStateAttributeName = "ChefState";
  public static final String _dinerStateAttributeName = "DinerState";
  public static final String _servingNameAttributeName = "ServingName";

  //interaction names
  public static final String _TransferAcceptedClassName = "HLAinteractionRoot.TransferAccepted";

  //parameter names
  public static final String _servingNameParameterName = "ServingName";
}
