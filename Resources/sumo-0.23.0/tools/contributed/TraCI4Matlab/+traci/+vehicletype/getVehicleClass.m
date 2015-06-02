function vehicleClass = getVehicleClass(typeID)
%getVehicleClass
%   vehicleClass = getVehicleClass(TYPEID) Returns the class of vehicles of 
%   this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getVehicleClass.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
vehicleClass = traci.vehicletype.getUniversal(constants.VAR_VEHICLECLASS, typeID);