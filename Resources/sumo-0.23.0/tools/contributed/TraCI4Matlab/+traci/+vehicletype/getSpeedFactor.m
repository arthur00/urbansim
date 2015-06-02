function speedFactor = getSpeedFactor(typeID)
%getSpeedFactor
%   speedFactor = getSpeedFactor(TYPEID) Returns the chosen speed factor for
%   vehicles of this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getSpeedFactor.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
speedFactor = traci.vehicletype.getUniversal(constants.VAR_SPEED_FACTOR, typeID);