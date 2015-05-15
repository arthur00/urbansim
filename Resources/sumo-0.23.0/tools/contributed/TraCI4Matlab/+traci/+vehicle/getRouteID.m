function routeID = getRouteID(vehID)
%getRouteID
%   routeID = getRouteID(VEHID) Returns the id of the route of the named 
%   vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getRouteID.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
routeID = traci.vehicle.getUniversal(constants.VAR_ROUTE_ID, vehID);