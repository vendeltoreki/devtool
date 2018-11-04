package com.liferay.devtool.bundles.model;

import com.liferay.devtool.bundles.BundleEntry;

public class ModelEvent {
	private EventType eventType;
	private BundleEntry bundleEntry;
	
	public ModelEvent() {
	}
	
	public ModelEvent(EventType eventType) {
		super();
		this.eventType = eventType;
	}

	public ModelEvent(EventType eventType, BundleEntry bundleEntry) {
		super();
		this.eventType = eventType;
		this.bundleEntry = bundleEntry;
	}

	public EventType getEventType() {
		return eventType;
	}
	
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	
	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}
	
	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}
}
