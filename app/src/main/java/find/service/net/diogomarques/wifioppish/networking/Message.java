package find.service.net.diogomarques.wifioppish.networking;

import java.io.Serializable;

import android.location.Location;

import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;

/**
 * Message envelope to be exchanged between devices.
 * 
 * <p>
 * Each Message contains a node ID, a timestamp indicating the 
 * original message send time, a geographical location and an optional text message. 
 * Messages also contain another attributes like device battery, number of steps given 
 * by victim among others.
 * 
 * <p>
 * This envelope was created to be suitable to be transmitted over a network. It 
 * uses Java Serialization to ensure that the contents are formatted correctly. 
 * 
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 *
 */
public class Message implements Serializable {

	/**
	 * Generated class serial number
	 */
	private static final long serialVersionUID = 4793280315313094725L;
	
	// identification
	private String nodeId;
	private String account;
	private long timestamp;
	
	// location
	private double latitude;
	private double longitude;
	private int accuracy;
	private long locationTimestamp;
	
	// sensor data
	private int battery;
	private int steps;
	private int screenOn;
	private boolean safe;
	
	// info
	private String message;
	private String status;
	private long statusTimestamp;
	private String origin;
	
	// target
	private String target;
	private double targetLatitude;
	private double targetLongitude;
	private int targetRadius;
	
	//Creates a new Message envelope with information regarding external conditions
	public Message(String nodeId, String account, long timestamp, String message, String status, long statusTimestamp, String origin) {
		this.nodeId = nodeId;
		this.account = account;
		this.timestamp = timestamp;
		
		this.battery = -1;
		this.safe = false;
		this.screenOn = -1;
		this.steps = -1;
		
		this.message = message;
		this.status = status;
		this.statusTimestamp = statusTimestamp;
		this.origin = origin;
	}
	
	/**
	 * Gets the device battery when this message was sent
	 * @return the battery
	 */
	public int getBattery() {
		return battery;
	}

	/**
	 * Sets the device battery information to be sent
	 * @param battery the battery to set
	 */
	public void setBattery(int battery) {
		this.battery = battery;
	}

	/**
	 * Gets the total of steps the victims made until the message was sent
	 * @return the steps
	 */
	public int getSteps() {
		return steps;
	}

	/**
	 * Sets the total number of steps for the victim
	 * @param steps the steps to set
	 */
	public void setSteps(int steps) {
		this.steps = steps;
	}

	/**
	 * Gets the total times the screen as turned on
	 * @return the screenOn
	 */
	public int getScreenOn() {
		return screenOn;
	}

	/**
	 * Sets the total times the screen as turned on
	 * @param screenOn the screenOn to set
	 */
	public void setScreenOn(int screenOn) {
		this.screenOn = screenOn;
	}

	/**
	 * Tells whenever the victim marked itself as safe
	 * @return the safe value
	 */
	public boolean isSafe() {
		return safe;
	}

	/**
	 * Sets the safe victim status
	 * @param safe the safe to set
	 */
	public void setSafe(boolean safe) {
		this.safe = safe;
	}

	/**
	 * Sets the user's location
	 * @param location the location to set
	 */
	public void setLocation(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		accuracy = (int)location.getAccuracy();
		locationTimestamp = location.getTime();
	}
	
	/**
	 * Gets the sender node identificator
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}
	
	/**
	 * Gets the user's google account name
	 * @return google account name
	 */
	public String getAccountName() {
		return account;
	}

	/**
	 * Gets the time when the message was sent
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Gets the latitude of the victim
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Gets the longitude of the victim
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Gets the location's accuracy
	 * @return the location's accuracy
	 */
	public int getLocationAccuracy() {
		return accuracy;
	}

	/**
	 * Gets the location's timestamp
	 * @return the location's timestamp
	 */
	public long getLocationTime() {
		return locationTimestamp;
	}
	
	/**
	 * Gets the textual message sent by the victim, if any
	 * @return the text message; empty if no text message was sent
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Sets the status of this message and the time when the status changed
	 * @param status the status to be set
	 * @param statusTime the time when the status was changed
	 */
	public void setStatus(String status, long statusTime) {
		this.status = status;
		this.statusTimestamp = statusTime;
	}
	
	/**
	 * Gets the status of this message
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Gets the timestamp of the last update of this messages' status
	 * @return the timestamp as specified
	 */
	public long getStatusTime() {
		return statusTimestamp;
	}
	
	/**
	 * Gets the origin of this message
	 * @return the origin of this message
	 */
	public String getOrigin() {
		return origin;
	}
	
	/**
	 * Gets the target of this message
	 * @return the target
	 */
	public String getTarget(){
		return target;
	}

	/**
	 * Gets the latitude of this message's target
	 * @return the target latitude
	 */
	public double getTargetLatitude(){
		return targetLatitude;
	}
	
	/**
	 * Gets the longitude of this message's target
	 * @return the target longitude
	 */
	public double getTargetLongitude(){
		return targetLongitude;
	}
	
	/**
	 * Gets the radius of this message's target
	 * @return the target radius
	 */
	public int getTargetRadius() {
		return targetRadius;
	}
	
	@Override
	public String toString() {
		return "Message [nodeId=" + nodeId + ", account= " + account
				+ ", timestamp=" + timestamp + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", accuracy=" + accuracy
				+ ", locationTimestamp=" + locationTimestamp + ", battery="
				+ battery + ", steps=" + steps + ", screenOn=" + screenOn
				+ ", safe=" + safe + ", message=" + message + ", status="
				+ status + ", statusTimestamp=" + statusTimestamp
				+ ", origin=" + origin + ", target=" + target
				+ ", targetLatitude=" + targetLatitude + ", targetLongitutde="
				+ targetLongitude + ", targetRadius=" + targetRadius + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((account == null) ? 0 : account.hashCode());
		result = prime * result + accuracy;
		result = prime * result + battery;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ (int) (locationTimestamp ^ (locationTimestamp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		result = prime * result + (safe ? 1231 : 1237);
		result = prime * result + screenOn;
//		result = prime * result + ((status == null) ? 0 : status.hashCode());
//		result = prime * result
//				+ (int) (statusTimestamp ^ (statusTimestamp >>> 32));
		result = prime * result + steps;
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		temp = Double.doubleToLongBits(targetLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(targetLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(targetRadius);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (account == null) {
			if (other.account != null)
				return false;
		} else if (!account.equals(other.account))
			return false;
		if (accuracy != other.accuracy)
			return false;
		if (battery != other.battery)
			return false;
		if (Double.doubleToLongBits(latitude) != Double
				.doubleToLongBits(other.latitude))
			return false;
		if (locationTimestamp != other.locationTimestamp)
			return false;
		if (Double.doubleToLongBits(longitude) != Double
				.doubleToLongBits(other.longitude))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (origin == null) {
			if (other.origin != null)
				return false;
		} else if (!origin.equals(other.origin))
			return false;
		if (safe != other.safe)
			return false;
		if (screenOn != other.screenOn)
			return false;
//		if (status == null) {
//			if (other.status != null)
//				return false;
//		} else if (!status.equals(other.status))
//			return false;
//		if (statusTimestamp != other.statusTimestamp)
//			return false;
		if (steps != other.steps)
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (Double.doubleToLongBits(targetLatitude) != Double
				.doubleToLongBits(other.targetLatitude))
			return false;
		if (Double.doubleToLongBits(targetLongitude) != Double
				.doubleToLongBits(other.targetLongitude))
			return false;
		if (Double.doubleToLongBits(targetRadius) != Double
				.doubleToLongBits(other.targetRadius))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}
}
