function emissionClass = getEmissionClass(typeID)
%getEmissionClass
%   emissionClass = getEmissionClass(TYPEID) Returns the emission class of 
%   vehicles of this type.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getEmissionClass.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
emissionClass = traci.vehicletype.getUniversal(constants.VAR_EMISSIONCLASS, typeID);