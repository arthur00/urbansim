function speedDeviation = getSpeedDeviation(typeID)
%getSpeedDeviation
%   speedDeviation = getSpeedDeviation(TYPEID) Returns the maximum speed 
%   deviation of vehicles of this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getSpeedDeviation.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
speedDeviation = traci.vehicletype.getUniversal(constants.VAR_SPEED_DEVIATION, typeID);