package se.pitch.sushifederation.manager;

public class ManagerNames {
  //labels of the synchronization points used in lifecycle protocol
  public static final String _readyToPopulate = "ReadyToPopulate";
  public static final String _readyToRun = "ReadyToRun";
  public static final String _readyToResign = "ReadyToResign";

  //names of object and interaction classes
  public static final String _FederateClassName = "HLAobjectRoot.HLAmanager.HLAfederate";
  public static final String _SimulationEndsClassName
    = "HLAinteractionRoot.HLAmanager.SimulationEnds";

  //attributes
  public static final String _FederateHandleAttributeName = "HLAfederateHandle";
  public static final String _FederateTypeAttributeName = "HLAfederateType";
  public static final String _FederateHostAttributeName = "HLAfederateHost";

  //federate type
  public static final String _federateType = "Manager";
}
