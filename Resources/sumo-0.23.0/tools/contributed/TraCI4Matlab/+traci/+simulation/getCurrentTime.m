function currentTime = getCurrentTime()
%getCurrentTime
%   currentTime = getCurrentTime() Returns the current simulation time in 
%   ms. 

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getCurrentTime.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
currentTime = traci.simulation.getUniversal(constants.VAR_TIME_STEP);