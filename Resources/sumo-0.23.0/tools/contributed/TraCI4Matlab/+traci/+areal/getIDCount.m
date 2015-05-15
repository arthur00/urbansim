function IDCount = getIDCount()
%IDCount = getIDCount() Get the number of aeral detectors in the SUMO network.  

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getIDCount.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
IDCount = traci.areal.getUniversal(constants.ID_COUNT, '');