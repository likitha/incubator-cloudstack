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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VMSnapshotResponse;
import com.cloud.vm.snapshot.VMSnapshot;

@Implementation(description = "List vm snapshot command", responseObject = VMSnapshotResponse.class, since = "5.0.0")
public class ListVmSnapshotCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class
            .getName());

    private static final String s_name = "listvmsnapshotresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName = "vm_snapshots")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "lists snapshot by snapshot ID")
    private Long id;

    @IdentityMapper(entityTableName = "cluster")
    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.LONG, description = "lists snapshot by cluster ID")
    private Long clusterId;

    @IdentityMapper(entityTableName = "domain")
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.LONG, description = "lists snapshot by domain ID")
    private Long domainId;

    @IdentityMapper(entityTableName = "vm_snapshots")
    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "lists snapshot by state")
    private String state;

    @IdentityMapper(entityTableName = "user")
    @Parameter(name = ApiConstants.USER_ID, type = CommandType.LONG, description = "lists snapshot by user ID")
    private Long userId;

    @IdentityMapper(entityTableName = "vm_instance")
    @Parameter(name = ApiConstants.VM_ID, type = CommandType.LONG, description = "the ID of the vm")
    private Long vmId;

    @IdentityMapper(entityTableName = "vm_snapshots")
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists snapshot by snapshot name or display name")
    private String vmSnapshotName;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public String getState() {
        return state;
    }

    public String getVmSnapshotName() {
        return vmSnapshotName;
    }

    public Long getVmId() {
        return vmId;
    }

    public Long getId() {
        return id;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long userId() {
        return userId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends VMSnapshot> result = _vmSnapshotService
                .listVMSnapshots(this);
        ListResponse<VMSnapshotResponse> response = new ListResponse<VMSnapshotResponse>();
        List<VMSnapshotResponse> snapshotResponses = new ArrayList<VMSnapshotResponse>();
        for (VMSnapshot r : result) {
            VMSnapshotResponse vmSnapshotResponse = _responseGenerator
                    .createVMSnapshotResponse(r);
            vmSnapshotResponse.setObjectName("vmSnapshot");
            snapshotResponses.add(vmSnapshotResponse);
        }
        response.setResponses(snapshotResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
