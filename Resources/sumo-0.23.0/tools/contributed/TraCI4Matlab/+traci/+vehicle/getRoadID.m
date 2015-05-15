function roadID = getRoadID(vehID)
%getRoadID
%   roadID = getRoadID(VEHID) Returns the id of the edge the named vehicle 
%   was at within the last step.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getRoadID.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
roadID = traci.vehicle.getUniversal(constants.VAR_ROAD_ID, vehID);