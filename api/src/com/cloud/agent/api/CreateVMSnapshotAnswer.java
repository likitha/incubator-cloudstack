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

package com.cloud.agent.api;

import java.util.List;
import com.cloud.agent.api.to.VMSnapshotVolumeTO;

public class CreateVMSnapshotAnswer extends Answer {

    private String snapshotUuid;
    private List<VMSnapshotVolumeTO> snapshotVolumeToList;

    public CreateVMSnapshotAnswer() {

    }

    public CreateVMSnapshotAnswer(CreateVMSnapshotCommand cmd, boolean success,
            String result) {
        super(cmd, success, result);
    }

    public CreateVMSnapshotAnswer(CreateVMSnapshotCommand cmd,
            String snapshotUUID, List<VMSnapshotVolumeTO> listVSVolumeTo) {
        super(cmd, true, "");
        this.snapshotUuid = snapshotUUID;
        this.snapshotVolumeToList = listVSVolumeTo;
    }

    public String getSnapshotUUID() {
        return snapshotUuid;
    }

    public List<VMSnapshotVolumeTO> getSnapshotVolumeTos() {
        return snapshotVolumeToList;
    }

}
