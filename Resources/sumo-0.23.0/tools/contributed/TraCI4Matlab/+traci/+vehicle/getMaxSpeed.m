function maxSpeed = getMaxSpeed(vehID)
%getMaxSpeed
%   maxSpeed = getMaxSpeed(VEHID) Returns the maximum speed in m/s of this 
%   vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getMaxSpeed.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
maxSpeed = traci.vehicle.getUniversal(constants.VAR_MAXSPEED, vehID);