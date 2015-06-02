function tau = getTau(typeID)
%getTau
%   tau = getTau(TYPEID) Returns the driver's reaction time in s for 
%   vehicles of this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getTau.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
tau = traci.vehicletype.getUniversal(constants.VAR_TAU, typeID);