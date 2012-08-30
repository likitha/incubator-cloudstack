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
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vm_snapshots")
public class VMSnapshotVO implements VMSnapshot, Identity {
    @Id
    @TableGenerator(name = "vm_snapshots_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_snapshots_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "name")
    String name;

    @Column(name = "display_name")
    String displayName;

    @Column(name = "description")
    String description;

    @Column(name = "vm_id")
    long vmId;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "service_offering_id")
    long serviceOfferingId;

    @Column(name = "snapshot_uuid")
    String snapshotUUID;

    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Status state;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "memory")
    Boolean memory;

    public VMSnapshotVO() {

    }

    public void setSnapshotUUID(String snapshotUUID) {
        this.snapshotUUID = snapshotUUID;
    }

    public String getSnapshotUUID() {
        return this.snapshotUUID;
    }

    public Date getRemoved() {
        return removed;
    }

    public VMSnapshotVO(Long accountId, Long domainId, Long vmId,
            String description, String vmSnapshotName, String vsDisplayName,
            Long serviceOfferingId, Boolean memory) {
        this.accountId = accountId;
        this.domainId = domainId;
        this.vmId = vmId;
        this.state = Status.Creating;
        this.description = description;
        this.name = vmSnapshotName;
        this.displayName = vsDisplayName;
        this.serviceOfferingId = serviceOfferingId;
        this.memory = memory;
    }

    public Boolean memory() {
        return memory;
    }

    @Override
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getvmId() {
        return vmId;
    }

    public void setvmId(Long vmId) {
        this.vmId = vmId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Status getState() {
        return state;
    }

    public void setState(Status state) {
        this.state = state;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
