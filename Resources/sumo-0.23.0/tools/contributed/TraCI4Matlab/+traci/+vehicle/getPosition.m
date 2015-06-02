function position = getPosition(vehID)
%getPosition
%   position = getPosition(VEHID) Returns the x,y position of the named 
%   vehicle within the last step.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getPosition.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
position = traci.vehicle.getUniversal(constants.VAR_POSITION, vehID);