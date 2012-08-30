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

package com.cloud.vm.snapshot.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.Volume;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.snapshot.VMSnapshotVolumeVO;

@Local(value = { VMSnapshotVolumeDao.class })
public class VMSnapshotVolumeDaoImpl extends
        GenericDaoBase<VMSnapshotVolumeVO, Long> implements VMSnapshotVolumeDao {

    private final SearchBuilder<VMSnapshotVolumeVO> SnapshotIdSearch;
    private final SearchBuilder<VMSnapshotVolumeVO> RootSnapshotIdSearch;

    @Override
    public List<VMSnapshotVolumeVO> findByVMSnapshot(Long vmSnapshotId) {
        SearchCriteria<VMSnapshotVolumeVO> sc = SnapshotIdSearch.create();
        sc.setParameters("vm_snapshot_id", vmSnapshotId);
        return listBy(sc, null);
    }

    protected VMSnapshotVolumeDaoImpl() {
        SnapshotIdSearch = createSearchBuilder();
        SnapshotIdSearch.and("vm_snapshot_id", SnapshotIdSearch.entity()
                .getVmSnapshotId(), SearchCriteria.Op.EQ);
        SnapshotIdSearch.done();

        RootSnapshotIdSearch = createSearchBuilder();
        RootSnapshotIdSearch.and("vm_snapshot_id", RootSnapshotIdSearch
                .entity().getVmSnapshotId(), SearchCriteria.Op.EQ);
        RootSnapshotIdSearch.and("volume_type", RootSnapshotIdSearch.entity()
                .getVolumeType(), SearchCriteria.Op.EQ);
        RootSnapshotIdSearch.done();
    }

    @Override
    public VMSnapshotVolumeVO findRootByVMSnapshot(Long vmSnapshotId) {
        SearchCriteria<VMSnapshotVolumeVO> sc = RootSnapshotIdSearch.create();
        sc.setParameters("vm_snapshot_id", vmSnapshotId);
        sc.setParameters("volume_type", Volume.Type.ROOT);
        return findOneBy(sc);
    }
}
