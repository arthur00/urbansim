function program = getProgram(tlsID)
%getProgram
%   program = getProgram(TLSID) Returns the id of the current program. 

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getProgram.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
program = traci.trafficlights.getUniversal(constants.TL_CURRENT_PROGRAM, tlsID);