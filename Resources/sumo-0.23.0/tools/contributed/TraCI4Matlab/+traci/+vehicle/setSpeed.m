function setSpeed(vehID, speed)
%setSpeed
%   setSpeed(VEHID,SPEED) Sets the speed in m/s for the named vehicle 
%   within the last step.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: setSpeed.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
traci.sendDoubleCmd(constants.CMD_SET_VEHICLE_VARIABLE, constants.VAR_SPEED, vehID, speed);