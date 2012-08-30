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
// under the License
package com.cloud.agent.api;

import java.util.List;

import com.cloud.agent.api.to.VMSnapshotVolumeTO;

public class DeleteVMSnapshotCommand extends Command {
    private String vmName;
    private String snapshotName;
    private List<VMSnapshotVolumeTO> snapshotVolumeToList;
    private String snapshotUuid;

    protected DeleteVMSnapshotCommand() {

    }

    public DeleteVMSnapshotCommand(String _vmName, String _snapshotName,
            String snapshotUUID, List<VMSnapshotVolumeTO> snapshotVolumeToList) {
        this.vmName = _vmName;
        this.snapshotName = _snapshotName;
        this.snapshotUuid = snapshotUUID;
        this.snapshotVolumeToList = snapshotVolumeToList;
    }

    public String getVmName() {
        return vmName;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public List<VMSnapshotVolumeTO> getSnapshotVolumeTos() {
        return snapshotVolumeToList;
    }

    public String getSnapshotUUID() {
        return snapshotUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
