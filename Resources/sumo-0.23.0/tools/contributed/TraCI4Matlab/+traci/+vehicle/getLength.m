function length = getLength(vehID)
%getLength
%   length = getLength(VEHID) Returns the length in m of the given vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getLength.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
length = traci.vehicle.getUniversal(constants.VAR_LENGTH, vehID);