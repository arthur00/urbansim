package urbansim.sumo;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;

class AddVehicle
{
	static InteractionClassHandle handle;
	static ParameterHandle vname;
	static ParameterHandle vtype;
	static ParameterHandle source;
	static ParameterHandle dname;
}

class CreateObject
{
	static InteractionClassHandle handle;
	static ParameterHandle vid;
	static ParameterHandle vtype;
	static ParameterHandle pos;
}

class DeleteObject
{
	static InteractionClassHandle handle;
	static ParameterHandle vid;
}
