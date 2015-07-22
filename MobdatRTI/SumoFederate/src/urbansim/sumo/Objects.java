
package urbansim.sumo;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ser.std.ToStringSerializer;



import org.jgroups.annotations.Immutable;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;



//This class is used in the Federate to descrive each instance of the objects
//There are 2 atributes, the handle that is given by The RTI when the object is created 
//and what type is this object - TrafficLight or Vehicle
class ObjectReferencesByID{
	
	public ObjectInstanceHandle handle;
	public String type;
	
	public ObjectReferencesByID(ObjectInstanceHandle objectHandle, String type) {
		this.handle = objectHandle;
		this.type = type;
	}
	
	public String toString(){
		return "Handle: "+handle+" type: "+type;
	}
}

class ObjectReferencesByHandle{
	
	public Integer id;
	public String type;
	
	public ObjectReferencesByHandle(Integer id, String type) {
		this.id = id;
		this.type = type;
	}
	
	public String toString(){
		return "id: "+id+" type: "+type;
	}
}



//This class is used to be a part of the definition of the objects (Vehicle/TrafficLight)
//it is used to describe each attribute of those classes in a dictionary. 
class ObjectData {
	
	public AttributeHandle handle;
	public String name;
	public String type;
	
	ObjectData(AttributeHandle handle,String name, String type){
		this.handle = handle;
		this.name = name;
		this.type = type;
	}
}


class Vehicle {
	
	static ObjectClassHandle handle;
	static AttributeHandle position;
	static AttributeHandle angle;
	static AttributeHandle velocity;
	static AttributeHandle vname;
	static AttributeHandle vtype;
	static Map<String,ObjectData > ObjectDataByName;
	static Map<AttributeHandle,ObjectData > ObjectDataByHandle;

}

class TrafficLight{
	
	static ObjectClassHandle handle;
	static AttributeHandle status;
	static AttributeHandle position;
	static AttributeHandle id;
	static Map<String,ObjectData > ObjectDataByName;
	static Map<AttributeHandle,ObjectData > ObjectDataByHandle;
	
}














