function deltaT = getDeltaT()
%getDeltaT
%   deltaT = getDeltaT() Returns the time-step of the simulation in ms. 

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getDeltaT.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
deltaT = traci.simulation.getUniversal(constants.VAR_DELTA_T);