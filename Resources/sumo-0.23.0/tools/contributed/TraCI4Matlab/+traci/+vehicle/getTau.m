function tau = getTau(vehID)
%getTau
%   tau = getTau(VEHID) Returns the driver's reaction time in s for this 
%   vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getTau.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
tau = traci.vehicle.getUniversal(constants.VAR_TAU, vehID);