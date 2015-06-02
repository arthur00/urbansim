function setSpeedFactor(vehID, factor)
%setSpeedFactor
%   setSpeedFactor(VEHID,FACTOR) Sets the speed factor for the named
%   vehicle within the last step.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: setSpeedFactor.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
traci.sendDoubleCmd(constants.CMD_SET_VEHICLE_VARIABLE, constants.VAR_SPEED_FACTOR, vehID, factor);