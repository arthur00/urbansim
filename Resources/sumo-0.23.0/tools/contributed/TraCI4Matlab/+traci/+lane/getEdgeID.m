function edgeID = getEdgeID(laneID)
%getEdgeID
%   edgeID = getEdgeID(LANEID) Returns the id of the edge the lane belongs 
%   to.

%   Copyright 2015 Universidad Nacional de Colombia,
%   Politecnico Jaime Isaza Cadavid.
%   Authors: Andres Acosta, Jairo Espinosa, Jorge Espinosa.
%   $Id: getEdgeID.m 20 2015-03-02 16:52:32Z afacostag $

import traci.constants
edgeID = traci.lane.getUniversal(constants.LANE_EDGE_ID, laneID);