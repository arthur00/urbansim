
package urbansim.sumo;


import java.util.Map;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ParameterHandle;


class InteractionData {
	
	public ParameterHandle handle;
	public String name;
	public String type;
	
	InteractionData(ParameterHandle handle,String name, String type){
		this.handle = handle;
		this.name = name;
		this.type = type;
	}
}

class AddVehicle
{
	static InteractionClassHandle handle;
	static ParameterHandle vname;
	static ParameterHandle vtype;
	static ParameterHandle source;
	static ParameterHandle dname;
	static Map<ParameterHandle,InteractionData> InteractionDataByHandle;
	
}

class CreateObject
{
	static InteractionClassHandle handle;
	static ParameterHandle vid;
	static ParameterHandle vtype;
	static ParameterHandle position;
	static Map<ParameterHandle,InteractionData> InteractionDataByHandle;
}

class DeleteObject
{
	static InteractionClassHandle handle;
	static ParameterHandle vid;
	static Map<ParameterHandle,InteractionData> InteractionDataByHandle;
}



class InductionLoop
{
	static InteractionClassHandle handle;
	static ParameterHandle id;
	static ParameterHandle count;
	static Map<ParameterHandle,InteractionData> InteractionDataByHandle;
}

