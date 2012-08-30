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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.UserContext;
import com.cloud.uservm.UserVm;
import com.cloud.vm.snapshot.VMSnapshot;

@Implementation(description = "Revert from vmsnapshot.", responseObject = SuccessResponse.class)
public class RevertToSnapshotCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger
            .getLogger(RevertToSnapshotCmd.class.getName());
    private static final String s_name = "reverttosnapshotresponse";

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING)
    private String accountName;

    @IdentityMapper(entityTableName = "domain")
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.LONG)
    private Long domainId;

    @IdentityMapper(entityTableName = "vm_snapshots")
    @Parameter(name = ApiConstants.VM_SNAPSHOT_ID, type = CommandType.LONG, required = true)
    private Long vmSnapShotId;

    private Long vmId;

    public Long getVmId() {
        return vmId;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getVmSnapShotId() {
        return vmSnapShotId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "vm_snapshots";
    }

    @Override
    public long getEntityOwnerId() {
        VMSnapshot vmSnapshot = _vmSnapshotService
                .getVMSnapshotById(getVmSnapShotId());
        if (vmSnapshot == null) {
            throw new InvalidParameterValueException(
                    "Unable to find the snapshot by id=" + getVmSnapShotId());
        }
        UserVm userVM = _userVmService.getUserVm(vmSnapshot.getvmId());
        this.vmId = vmSnapshot.getvmId();
        return userVM.getAccountId();
    }

    @Override
    public void execute() {
        UserContext.current().setEventDetails(
                "vmsnapshot id: " + getVmSnapShotId());
        UserVm result = _vmSnapshotService.revertToSnapshot(this);
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(
                    "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR,
                    "Failed to revert vm snapshot");
        }
    }

    @Override
    public String getEventDescription() {
        return "Revert from VM snapshot: " + getVmSnapShotId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_SNAPSHOT_REVERT;
    }

}
