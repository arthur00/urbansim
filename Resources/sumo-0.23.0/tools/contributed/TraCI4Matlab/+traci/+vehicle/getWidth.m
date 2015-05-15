function width = getWidth(vehID)
%getWidth
%   width = getWidth(VEHID) Returns the width in m of this vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getWidth.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
width = traci.vehicle.getUniversal(constants.VAR_WIDTH, vehID);