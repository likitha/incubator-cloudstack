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

package com.cloud.vm.snapshot;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.cloud.api.Identity;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vm_snapshot_volume")
public class VMSnapshotVolumeVO implements VMSnapshotVolume, Identity {
    @Id
    @TableGenerator(name = "vm_snapshot_volume_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_snapshot_volume_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    Long id;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "volume_path")
    String volumePath;

    @Column(name = "vm_snapshot_id")
    Long vmSnapshotId;

    @Column(name = "snapshot_of")
    Long snapshotOf;

    @Column(name = "volume_type")
    @Enumerated(EnumType.STRING)
    Type volumeType = Volume.Type.UNKNOWN;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public Long getSnapshotOf() {
        return snapshotOf;
    }

    public void setSnapshotOf(Long snapshotOf) {
        this.snapshotOf = snapshotOf;
    }

    public VMSnapshotVolumeVO() {

    }

    public VMSnapshotVolumeVO(Long vmSnapshotId, Type volumeType,
            Long snapshotOf) {
        this.vmSnapshotId = vmSnapshotId;
        this.volumeType = volumeType;
        this.snapshotOf = snapshotOf;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Type getVolumeType() {
        return volumeType;
    }

    public void setVolumeTyep(Type volumeType) {
        this.volumeType = volumeType;
    }

    @Override
    public Long getVmSnapshotId() {
        return vmSnapshotId;
    }

    public void setVmSnapshotId(Long vmSnapshotId) {
        this.vmSnapshotId = vmSnapshotId;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setVolumePath(String volumePath) {
        this.volumePath = volumePath;
    }

    @Override
    public String getVolumePath() {
        return volumePath;
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

}
