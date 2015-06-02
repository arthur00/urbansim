function typeID = getTypeID(vehID)
%getTypeID
%   typeID = getTypeID(VEHID) Returns the id of the type of the named 
%   vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getTypeID.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
typeID = traci.vehicle.getUniversal(constants.VAR_TYPE, vehID);