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

package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.cloud.vm.snapshot.VMSnapshot.Status;
import com.google.gson.annotations.SerializedName;

public class VMSnapshotResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the vm snapshot")
    private IdentityProxy id = new IdentityProxy("vm_snapshots");

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the vm snapshot")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the vm snapshot")
    private Status state;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the vm snapshot")
    private String description;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the vm snapshot")
    private String displayName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID of the vm snapshot")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("vmid")
    @Param(description = "the vm ID of the vm snapshot")
    private IdentityProxy vmId = new IdentityProxy("vm_instance");

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the vm snapshot")
    private Date created;

    @Override
    public Long getObjectId() {
        return getId();
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setState(Status state) {
        this.state = state;
    }

    public Status getState() {
        return state;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }

    public void setVmId(Long vmId) {
        this.vmId.setValue(vmId);
    }

}
