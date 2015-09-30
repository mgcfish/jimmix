package jimmix.action; 

import javax.management.ObjectName;

import jimmix.util.TypeConvertor;
import jimmix.util.Log;
import jimmix.proxy.ProxyType;

public class SetAction extends Action {

	private ObjectName mbean = null;
	private String attribute = null;
	private String value = null;
	
	public SetAction(String url, ProxyType type, String mbean, String attribute, String value) throws Exception {
		super(url, type);
		this.mbean = new ObjectName(mbean);
		this.attribute = attribute;
		this.value = value;
	}

	public void invoke() throws Exception {
		Log.log("setting the property " + this.attribute + " with " + this.value + " from " + this.mbean.getCanonicalName() + "");
		this.proxy.set(this.mbean, this.attribute, this.value);
    Log.log("done");
	}
}
