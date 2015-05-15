function maxSpeed = getMaxSpeed(typeID)
%getMaxSpeed
%   maxSpeed = getMaxSpeed(TYPEID) Returns the maximum speed in m/s of 
%   vehicles of this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getMaxSpeed.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
maxSpeed = traci.vehicletype.getUniversal(constants.VAR_MAXSPEED, typeID);