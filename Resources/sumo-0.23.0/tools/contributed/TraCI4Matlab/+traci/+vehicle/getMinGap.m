function minGap = getMinGap(vehID)
%getMinGap
%   minGap = getMinGap(VEHID) Returns the offset (gap to front vehicle if 
%   halting) of this vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getMinGap.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
minGap = traci.vehicle.getUniversal(constants.VAR_MINGAP, vehID);