function type = getType(poiID)
%getType
%   type = getType(POIID) Returns the (abstract) type of the poi.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getType.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
type = traci.poi.getUniversal(constants.VAR_TYPE, poiID);