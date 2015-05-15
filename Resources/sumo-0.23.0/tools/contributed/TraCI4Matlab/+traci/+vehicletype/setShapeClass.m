function setShapeClass(typeID, clazz)
%setShapeClass
%   setShapeClass(TYPEID,CLASS) Sets the shape class of vehicles of this 
%   type. 

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: setShapeClass.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
traci.sendStringCmd(constants.CMD_SET_VEHICLETYPE_VARIABLE, constants.VAR_SHAPECLASS, typeID, clazz);