function isvalid = isRouteValid(vehID)
%isRouteValid
%   isvalid = isRouteValid(VEHID) Determine whether the current route of the
%   vehicle is valid (i.e. all the edges along the route are connected).

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: isRouteValid.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
isvalid = traci.vehicle.getUniversal(constants.VAR_ROUTE_VALID, vehID);