// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.agent.api.to;

import com.cloud.storage.Volume;

public class VMSnapshotVolumeTO {

	String volumePath; // cloud.volumes.path
	String snapshotVolumePath; // cloud.vm_snapshot_volume.path
	Long snapshotOf; // id in cloud.volumes
	Long snapshotVolumeId; // id in cloud.vm_snapshot_volume
	Volume.Type volumeType; // root or data
	Long deviceId; // device id in xenserver (1,2...); cloud.volumes.device_id
	String newVolumePath; // new volume path to update cloud.volumes
	
	public void setNewVolumePath(String newVolumePath){
		this.newVolumePath = newVolumePath;
	}
	
	public String getNewVolumePath(){
		return newVolumePath;
	}
	
	public Long getDeviceId(){
		return deviceId;
	}
	
	public Volume.Type getVolumeType(){
		return volumeType;
	}
	
	protected VMSnapshotVolumeTO(){
		
	}
	
	public Long getSnapshotVolumeId(){
		return snapshotVolumeId;
	}
	
	public VMSnapshotVolumeTO(Long snapshotVolumeId, String volumePath, String snapshotVolumePath){
		this.snapshotVolumeId = snapshotVolumeId;
		this.volumePath = volumePath;
		this.snapshotVolumePath = snapshotVolumePath;
	}
	
    public VMSnapshotVolumeTO(Long snapshotVolumeId, String volumePath,
            String snapshotVolumePath, Volume.Type volumeType, Long deviceId) {
        this.snapshotVolumeId = snapshotVolumeId;
        this.volumePath = volumePath;
		this.snapshotVolumePath = snapshotVolumePath;
		this.volumeType = volumeType;
		this.deviceId = deviceId;
	}
	
	public VMSnapshotVolumeTO(Long snapshotVolumeId, String volumePath, Long deviceId) {
		this.snapshotVolumeId = snapshotVolumeId;
		this.volumePath = volumePath;
		this.deviceId = deviceId;
	}

	public String getVolumePath(){
		return volumePath;
	}
	
	public String getSnapshotVolumePath(){
		return snapshotVolumePath;
	}
	
	public void setSnapshotVolumePath(String snapshotVolumePath){
		this.snapshotVolumePath = snapshotVolumePath;
	}
}
