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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.snapshot.VMSnapshot.Status;
import com.cloud.vm.snapshot.VMSnapshotVO;

@Local(value = { VMSnapshotDao.class })
public class VMSnapshotDaoImpl extends GenericDaoBase<VMSnapshotVO, Long>
        implements VMSnapshotDao {

    private final SearchBuilder<VMSnapshotVO> SnapshotSearch;
    private final SearchBuilder<VMSnapshotVO> ExpungingSnapshotSearch;
    private final SearchBuilder<VMSnapshotVO> SnapshotStatusSearch;

    protected VMSnapshotDaoImpl() {
        SnapshotSearch = createSearchBuilder();
        SnapshotSearch.and("vm_id", SnapshotSearch.entity().getvmId(),
                SearchCriteria.Op.EQ);
        SnapshotSearch.done();

        ExpungingSnapshotSearch = createSearchBuilder();
        ExpungingSnapshotSearch.and("state", ExpungingSnapshotSearch.entity()
                .getState(), SearchCriteria.Op.EQ);
        ExpungingSnapshotSearch.and("removed", ExpungingSnapshotSearch.entity()
                .getRemoved(), SearchCriteria.Op.NULL);
        ExpungingSnapshotSearch.done();

        SnapshotStatusSearch = createSearchBuilder();
        SnapshotStatusSearch.and("vm_id", SnapshotStatusSearch.entity()
                .getvmId(), SearchCriteria.Op.EQ);
        SnapshotStatusSearch.and("state", SnapshotStatusSearch.entity()
                .getState(), SearchCriteria.Op.IN);
        SnapshotStatusSearch.done();
    }

    @Override
    public List<VMSnapshotVO> findByVm(Long vmId) {
        SearchCriteria<VMSnapshotVO> sc = SnapshotSearch.create();
        sc.setParameters("vm_id", vmId);
        return listBy(sc, null);
    }

    @Override
    public List<VMSnapshotVO> listExpungingSnapshot() {
        SearchCriteria<VMSnapshotVO> sc = ExpungingSnapshotSearch.create();
        sc.setParameters("state", Status.Expunging);
        return listBy(sc, null);
    }

    @Override
    public List<VMSnapshotVO> listByInstanceId(Long vmId, Status... status) {
        SearchCriteria<VMSnapshotVO> sc = SnapshotStatusSearch.create();
        sc.setParameters("vm_id", vmId);
        sc.setParameters("state", (Object[]) status);
        return listBy(sc, null);
    }

}
