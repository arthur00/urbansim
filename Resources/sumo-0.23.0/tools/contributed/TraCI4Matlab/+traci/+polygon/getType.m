function type = getType(polygonID)
%getType
%   type = getType(POLYGONID) Returns the (abstract) type of the polygon.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getType.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
type = traci.polygon.getUniversal(constants.VAR_TYPE, polygonID);