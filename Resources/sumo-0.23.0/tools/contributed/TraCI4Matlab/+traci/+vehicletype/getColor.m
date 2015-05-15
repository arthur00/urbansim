function color = getColor(typeID)
%getColor
%   color = getColor(TYPEID)Returns the color of this type of vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getColor.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
color = traci.vehicletype.getUniversal(constants.VAR_COLOR, typeID);