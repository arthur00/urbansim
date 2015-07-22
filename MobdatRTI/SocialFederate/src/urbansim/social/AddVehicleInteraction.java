package urbansim.social;

import java.io.*;
import java.net.*;
import java.util.Arrays;

class AddVehicleInteraction
{

	private String vname;
	private String evt_type;
	private String vtype;
	private String sname;
	private String dname;
	private int id;
	private int[] array;
	private String[] array_list;
	private float float_number;
	


	public AddVehicleInteraction()
	{
		
	}
	
	/*
	public AddVehicleInteraction(String vname)
	{
		this.vname = vname;
		this.vtype = null;
		this.evt_type = null;
		this.sname = null;
		this.dname = null;
	}
	
	public AddVehicleInteraction(String vname,String vtype){
		this.vname = vname;
		this.vtype = vtype;
		this.evt_type = null;
		this.sname = null;
		this.dname = null;
	}
	
	*/
	
	public AddVehicleInteraction(String dname,String evt_type,String vtype,String sname, String vname, int id , int[] array, String[] array_list,float float_number){
		this.vname = vname;
		this.vtype = vtype;
		this.evt_type = evt_type;
		this.sname = sname;
		this.dname = dname;
		this.id = id;
		this.array = array;
		this.array_list = array_list;
		this.float_number = float_number;
	}
	
	public String getVname() {
		return vname;
	}

	public void setVname(String vname) {
		this.vname = vname;
	}

	public String getEvt_type() {
		return evt_type;
	}

	public void setEvt_type(String evt_type) {
		this.evt_type = evt_type;
	}

	public String getVtype() {
		return vtype;
	}

	public void setVtype(String vtype) {
		this.vtype = vtype;
	}

	public String getSname() {
		return sname;
	}

	public void setSname(String sname) {
		this.sname = sname;
	}

	public String getDname() {
		return dname;
	}

	public void setDname(String dname) {
		this.dname = dname;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public float getFloat_number() {
		return float_number;
	}

	public void setFloat_number(float float_number) {
		this.float_number = float_number;
	}
	
	public int[] getArray() {
		return array;
	}

	public void setArray(int[] array) {
		this.array = array;
	}
	
	public String[] getArray_list() {
		return array_list;
	}

	public void setArray_list(String[] array_list) {
		this.array_list = array_list;
	}

	
	
	 @Override
	   public String toString()
	   {		
	      return "Vname: " + vname + ", evt_type: " + evt_type + ", vtype: " + vtype + ", Sname: " + sname + ", Dname: " + dname + ", ID: " + id + ", Float:" + float_number+ ", Array: " + array[0] + 
	    		  ", Array_list: " + array_list[0] ;
	   }
	

}