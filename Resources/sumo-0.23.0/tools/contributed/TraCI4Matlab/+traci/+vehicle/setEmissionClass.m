function setEmissionClass(vehID, clazz)
%setEmissionClass
%   setEmissionClass(VEHID,CLASS) Sets the emission class for this vehicle.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: setEmissionClass.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
traci.sendStringCmd(constants.CMD_SET_VEHICLE_VARIABLE, constants.VAR_EMISSIONCLASS, vehID, clazz);