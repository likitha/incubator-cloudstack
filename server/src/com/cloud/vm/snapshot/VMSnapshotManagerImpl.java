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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.GetVolumesChangedAnswer;
import com.cloud.agent.api.GetVolumesChangedCommand;
import com.cloud.agent.api.RevertToSnapshotAnswer;
import com.cloud.agent.api.RevertToSnapshotCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VMSnapshotVolumeTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.api.commands.CreateVMSnapshotCmd;
import com.cloud.api.commands.DeleteVMSnapshotCmd;
import com.cloud.api.commands.ListVmSnapshotCmd;
import com.cloud.api.commands.RevertToSnapshotCmd;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.ha.HighAvailabilityManager.WorkType;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotVolumeDao;

@Local(value = { VMSnapshotManager.class, VMSnapshotService.class })
public class VMSnapshotManagerImpl implements VMSnapshotManager,
        VMSnapshotService, Manager {
    private static final Logger s_logger = Logger
            .getLogger(VMSnapshotManagerImpl.class);
    String _name;
    @Inject
    protected VMSnapshotDao _vmSnapshotDao;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected UserVmDao _userVMDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected VMSnapshotVolumeDao _vmSnapshotVolumeDao;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected VolumeDao _volsDao = null;
    @Inject
    protected GuestOSDao _guestOSDao = null;
    @Inject
    protected VMTemplateDao _templateDao = null;
    @Inject
    protected VMTemplateDetailsDao _templateDetailsDao = null;
    @Inject
    private StoragePoolDao _storagePoolDao;
    @Inject
    protected NetworkManager _networkMgr = null;
    @Inject
    protected NetworkDao _networkDao = null;
    @Inject
    protected SecurityGroupManager _securityGroupMgr;
    @Inject
    protected SecurityGroupDao _securityGroupDao;
    @Inject
    protected ConfigurationManager _configMgr = null;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected SSHKeyPairDao _sshKeyPairDao;
    @Inject
    protected UserVmDao _vmDao = null;
    @Inject
    protected UsageEventDao _usageEventDao;
    @Inject
    protected VMTemplateDao _vmTemplateDao;
    @Inject
    UserVmManager _userVmMgr;
    @Inject
    protected SnapshotDao _snapshotDao = null;
    @Inject
    protected HighAvailabilityManager _haMgr;
    @Inject
    GuestOSDao _guestOsDao;

    private ConfigurationDao _configDao;
    protected String _instance;
    protected int _vmSnapshotMax;
    protected StateMachine2<State, VirtualMachine.Event, VirtualMachine> _stateMachine;
    ScheduledExecutorService _executor = null;
    int _snapshotCleanupInterval;

    Map<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _configDao = locator.getDao(ConfigurationDao.class);
        if (_configDao == null) {
            throw new ConfigurationException(
                    "Unable to get the configuration dao.");
        }
        s_logger.info("Snapshot Manager is configured.");

        Map<String, String> configs = _configDao.getConfiguration(
                "AgentManager", params);
        _instance = configs.get("instance.name");

        _vmSnapshotMax = NumbersUtil.parseInt(
                _configDao.getValue("vmsnapshot.max"), VMSNAPSHOTMAX);

        _stateMachine = VirtualMachine.State.getStateMachine();

        String workers = configs.get("vmsnapshot.expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks,
                new NamedThreadFactory("VMSnapshotManager-Scavenger"));

        String time = configs.get("vmsnapshot.expunge.interval");
        _snapshotCleanupInterval = NumbersUtil.parseInt(time, 86400);

        return true;
    }

    @Override
    public boolean start() {
        Random generator = new Random();
        int initialDelay = generator.nextInt(_snapshotCleanupInterval);
        _executor.scheduleWithFixedDelay(new VMSnapshotGarbageCollector(),
                initialDelay, _snapshotCleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<VMSnapshotVO> listVMSnapshots(ListVmSnapshotCmd cmd) {
        Account caller = UserContext.current().getCaller();
        List<Long> permittedAccounts = new ArrayList<Long>();

        boolean listAll = cmd.listAll();
        Long id = cmd.getId();
        Long vmId = cmd.getVmId();
        String state = cmd.getState();
        String keyword = cmd.getKeyword();
        String name = cmd.getVmSnapshotName();
        String accountName = cmd.getAccountName();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(),
                cmd.getProjectId(), permittedAccounts,
                domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject
                .third();

        Filter searchFilter = new Filter(VMSnapshotVO.class, "created", false,
                cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMSnapshotVO> sb = _vmSnapshotDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive,
                permittedAccounts, listProjectResourcesCriteria);

        sb.and("vm_id", sb.entity().getvmId(), SearchCriteria.Op.EQ);
        sb.and("domain_id", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("status", sb.entity().getState(), SearchCriteria.Op.IN);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("display_name", sb.entity().getDisplayName(),
                SearchCriteria.Op.EQ);
        sb.and("account_id", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<VMSnapshotVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive,
                permittedAccounts, listProjectResourcesCriteria);

        if (accountName != null && cmd.getDomainId() != null) {
            Account account = _accountMgr.getActiveAccountByName(accountName,
                    cmd.getDomainId());
            sc.setParameters("account_id", account.getId());
        }

        if (vmId != null) {
            sc.setParameters("vm_id", vmId);
        }

        if (domainId != null) {
            sc.setParameters("domain_id", domainId);
        }

        if (state == null) {
            VMSnapshot.Status[] status = { VMSnapshot.Status.Created,
                    VMSnapshot.Status.Creating, VMSnapshot.Status.Error,
                    VMSnapshot.Status.Expunging };
            sc.setParameters("status", (Object[]) status);
        } else {
            sc.setParameters("state", state);
        }

        if (name != null) {
            sc.setParameters("display_name", name);
        }

        if (keyword != null) {
            SearchCriteria<VMSnapshotVO> ssc = _vmSnapshotDao
                    .createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("display_name", SearchCriteria.Op.LIKE, "%" + keyword
                    + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword
                    + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        return _vmSnapshotDao.search(sc, searchFilter);
    }

    @Override
    @DB
    public VMSnapshot allocVMSnapshot(CreateVMSnapshotCmd cmd)
            throws ResourceAllocationException {
        Long vmId = cmd.getVmId();
        String vsDisplayName = cmd.getDisplayName();
        String vsDescription = cmd.getDescription();
        Boolean snapshotMemory = cmd.snapshotMemory();

        Account caller = UserContext.current().getCaller();
        final Transaction txn = Transaction.currentTxn();

        VMInstanceVO vmInstanceVo = _vmInstanceDao.findById(vmId);
        if (vmInstanceVo == null) {
            throw new InvalidParameterValueException(
                    "Creating vm snapshot failed due to vm:" + vmId
                            + " doesn't exist");
        }

        UserVmVO userVmVo = _userVMDao.findById(vmId);
        if (userVmVo == null) {
            throw new InvalidParameterValueException(
                    "Creating vm snapshot failed due to vm:" + vmId
                            + " is system vm");
        }

        if (vmInstanceVo.getState() != VirtualMachine.State.Running
                && vmInstanceVo.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException(
                    "Creating vm snapshot failed due to vm:" + vmId
                            + " is not in the running or Stopped state");
        }

        _accountMgr.checkAccess(caller, null, true, vmInstanceVo);

        if (_vmSnapshotDao.findByVm(vmId).size() >= _vmSnapshotMax) {
            throw new InvalidParameterValueException(
                    "Creating vm snapshot failed due to a vm can just have : "
                            + _vmSnapshotMax
                            + " snapshots. Please delete old ones");
        }

        List<VolumeVO> listVolumes = _volumeDao.findByInstance(vmId);
        for (VolumeVO volume : listVolumes) {
            List<SnapshotVO> activeSnapshots = _snapshotDao
                    .listByInstanceId(volume.getInstanceId(),
                            Snapshot.Status.Creating,
                            Snapshot.Status.CreatedOnPrimary,
                            Snapshot.Status.BackingUp);
            if (activeSnapshots.size() > 0) {
                throw new CloudRuntimeException(
                        "There is other active volume snapshot tasks on the instance to which the volume is attached, please try again later.");
            }
        }

        List<VMSnapshotVO> activeVMSnapshots = _vmSnapshotDao.listByInstanceId(
                vmId, VMSnapshot.Status.Creating);
        if (activeVMSnapshots.size() > 1) {
            throw new CloudRuntimeException(
                    "There is other active vm snapshot tasks on the instance to which the volume is attached, please try again later");
        }
        try {
            if (vmInstanceVo.getState() == VirtualMachine.State.Stopped) {
                snapshotMemory = false;
            }
            String timeString = DateUtil
                    .getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(),
                            DateUtil.YYYYMMDD_FORMAT);
            String vmSnapshotName = vmInstanceVo.getInstanceName()
                    + "_Snapshot_" + timeString;
            if (vsDisplayName == null) {
                vsDisplayName = vmSnapshotName;
            }

            txn.start();

            VMSnapshotVO vmSnapshotVo = new VMSnapshotVO(
                    vmInstanceVo.getAccountId(), vmInstanceVo.getDomainId(),
                    vmId, vsDescription, vmSnapshotName, vsDisplayName,
                    vmInstanceVo.getServiceOfferingId(), snapshotMemory);
            VMSnapshot vmSnapshot = _vmSnapshotDao.persist(vmSnapshotVo);
            if (vmSnapshot == null) {
                throw new CloudRuntimeException(
                        "Failed to create snapshot for vm: " + vmId);
            }
            List<VolumeVO> volumes = _volumeDao.findByInstance(vmId);
            for (VolumeVO volume : volumes) {
                VMSnapshotVolumeVO vmSnapshotVolumeVo = new VMSnapshotVolumeVO(
                        vmSnapshot.getId(), volume.getVolumeType(),
                        volume.getId());
                _vmSnapshotVolumeDao.persist(vmSnapshotVolumeVo);
            }
            txn.commit();
            return vmSnapshot;
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("create vm snapshot record failed for vm: " + vmId
                    + " due to " + msg);
            txn.rollback();
        } finally {
            txn.close();
        }

        return null;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_CREATE, eventDescription = "creating_vm_snapshot", async = true)
    public VMSnapshot creatVMSnapshot(CreateVMSnapshotCmd cmd) {
        Long vmId = cmd.getVmId();
        VMInstanceVO vmInstanceVo = _vmInstanceDao.findById(vmId);
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(cmd.getEntityId());
        CreateVMSnapshotAnswer answer = null;
        Boolean snapshotMemory = cmd.snapshotMemory();

        try {
            Long hostId = pickRunningHost(vmId);
            List<VMSnapshotVolumeVO> snapshotVolumeList = _vmSnapshotVolumeDao
                    .findByVMSnapshot(vmSnapshot.getId());
            List<VMSnapshotVolumeTO> snapshotVolumeTos = new ArrayList<VMSnapshotVolumeTO>();
            GuestOSVO guestOS = _guestOsDao.findById(vmInstanceVo
                    .getGuestOSId());
            for (VMSnapshotVolumeVO snapshotVolume : snapshotVolumeList) {
                VolumeVO volume = _volumeDao.findById(snapshotVolume
                        .getSnapshotOf());
                VMSnapshotVolumeTO snapshotVolumeTo = new VMSnapshotVolumeTO(
                        snapshotVolume.getId(), volume.getPath(),
                        volume.getDeviceId());
                snapshotVolumeTos.add(snapshotVolumeTo);
            }
            CreateVMSnapshotCommand ccmd = new CreateVMSnapshotCommand(
                    vmInstanceVo.getInstanceName(), vmSnapshot.getName(),
                    vmSnapshot.getDescription(), snapshotVolumeTos,
                    snapshotMemory, vmInstanceVo.getState(),
                    guestOS.getDisplayName());
            answer = (CreateVMSnapshotAnswer) sendToPool(hostId, ccmd);
            if (answer != null && answer.getResult()) {
                processAnswer(vmSnapshot, answer);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("create vm snapshot failed for vm: " + vmId
                    + " due to " + msg);
            throw new CloudRuntimeException(msg);
        } finally {
            if (answer == null || answer.getResult() == false
                    || vmSnapshot.getState() != VMSnapshot.Status.Created) {
                vmSnapshot.setState(VMSnapshot.Status.Error);
                _vmSnapshotDao.persist(vmSnapshot);
                List<VMSnapshotVolumeVO> listVolumes = _vmSnapshotVolumeDao
                        .findByVMSnapshot(vmSnapshot.getId());
                for (VMSnapshotVolumeVO volume : listVolumes) {
                    _vmSnapshotVolumeDao.remove(volume.getId());
                }
            }
        }
        return vmSnapshot;
    }

    @DB
    private void processAnswer(VMSnapshotVO vmSnapshot, Answer as) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            if (as instanceof CreateVMSnapshotAnswer) {
                CreateVMSnapshotAnswer answer = (CreateVMSnapshotAnswer) as;
                List<VMSnapshotVolumeTO> snapshotVolumeToList = answer
                        .getSnapshotVolumeTos();
                String snapshotUUID = answer.getSnapshotUUID();
                for (VMSnapshotVolumeTO snapshotVolumeTo : snapshotVolumeToList) {
                    VMSnapshotVolumeVO vsvolume = _vmSnapshotVolumeDao
                            .findById(snapshotVolumeTo.getSnapshotVolumeId());
                    vsvolume.setVolumePath(snapshotVolumeTo
                            .getSnapshotVolumePath());
                    _vmSnapshotVolumeDao.persist(vsvolume);
                }
                updateVolumePath(snapshotVolumeToList);
                vmSnapshot.setSnapshotUUID(snapshotUUID);
                vmSnapshot.setState(VMSnapshot.Status.Created);
                _vmSnapshotDao.persist(vmSnapshot);
            } else if (as instanceof RevertToSnapshotAnswer) {
                RevertToSnapshotAnswer answer = (RevertToSnapshotAnswer) as;
                List<VMSnapshotVolumeTO> snapshotVolumeToList = answer
                        .getSnapshotVolumeTos();
                updateVolumePath(snapshotVolumeToList);
                VMSnapshotVolumeTO snapshotVolumeTo = snapshotVolumeToList
                        .iterator().next();
                Long snapshotVolumeId = snapshotVolumeTo.getSnapshotVolumeId();
                VMSnapshotVolumeVO snapshotVolume = _vmSnapshotVolumeDao
                        .findById(snapshotVolumeId);
                VolumeVO volume = _volumeDao.findById(snapshotVolume
                        .getSnapshotOf());
                VMInstanceVO vm = _vmInstanceDao.findById(volume
                        .getInstanceId());
                vm.setState(answer.getVmState());
                _vmInstanceDao.persist(vm);
            } else if (as instanceof DeleteVMSnapshotAnswer) {
                DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer) as;
                List<VMSnapshotVolumeVO> snapshotVolumeVoList = _vmSnapshotVolumeDao
                        .findByVMSnapshot(vmSnapshot.getId());
                List<VMSnapshotVolumeTO> snapshotVolumeToList = answer
                        .getSnapshotVolumeTos();
                if (snapshotVolumeToList != null
                        && snapshotVolumeToList.size() > 0) {
                    updateVolumePath(snapshotVolumeToList);
                    for (VMSnapshotVolumeVO snapshotVolumeVo : snapshotVolumeVoList) {
                        _vmSnapshotVolumeDao.remove(snapshotVolumeVo.getId());
                    }
                }
                _vmSnapshotDao.remove(vmSnapshot.getId());
            }
            txn.commit();
        } catch (Exception e) {
            s_logger.error("error while process answer: " + as.getClass()
                    + " due to " + e.getMessage(), e);
            txn.rollback();
        } finally {
            txn.close();
        }
    }

    private void updateVolumePath(List<VMSnapshotVolumeTO> snapshotVolumeToList) {
        for (VMSnapshotVolumeTO snapshotVolumeTo : snapshotVolumeToList) {
            if (snapshotVolumeTo.getNewVolumePath() != null) {
                VMSnapshotVolumeVO snapshotVolumeVo = _vmSnapshotVolumeDao
                        .findById(snapshotVolumeTo.getSnapshotVolumeId());
                VolumeVO volume = _volumeDao.findById(snapshotVolumeVo
                        .getSnapshotOf());
                volume.setPath(snapshotVolumeTo.getNewVolumePath());
                _volumeDao.persist(volume);
            }
        }
    }

    private Answer sendToPool(Long hostId, Command cmd)
            throws AgentUnavailableException, OperationTimedoutException {
        long targetHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(
                hostId, cmd);
        Answer answer = _agentMgr.send(targetHostId, cmd);
        return answer;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_DELETE, eventDescription = "delete_vm_snapshot", async = true)
    public boolean deleteVMSnapshot(DeleteVMSnapshotCmd cmd) {
        Long vmSnapshotId = cmd.getVmSnapShotId();
        Account caller = UserContext.current().getCaller();

        VMSnapshot vmSnapshotCheck = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshotCheck == null) {
            throw new InvalidParameterValueException(
                    "unable to find the vm snapshot with id " + vmSnapshotId);
        }

        _accountMgr.checkAccess(caller, null, true, vmSnapshotCheck);

        if (VMSnapshot.Status.Created != vmSnapshotCheck.getState()
                && VMSnapshot.Status.Error != vmSnapshotCheck.getState()) {
            throw new InvalidParameterValueException(
                    "Can't delete the vm snapshotshot " + vmSnapshotId
                            + " due to it is not in Created or Error Status");
        }

        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        vmSnapshot.setState(VMSnapshot.Status.Expunging);
        _vmSnapshotDao.persist(vmSnapshot);

        return true;
    }

    @DB
    protected boolean deleteSnapshotInternal(Long vmId, Long vmSnapshotId) {
        VMInstanceVO vmInstanceVo = _vmInstanceDao.findById(vmId);
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(vmSnapshotId);
        try {
            Long hostId = pickRunningHost(vmId);
            String snapshotUUID = vmSnapshot.getSnapshotUUID();
            if (snapshotUUID == null) {
                s_logger.warn("snapshotUUID is null, mark as removed");
                _vmSnapshotDao.remove(vmSnapshotId);
                return true;
            }

            List<VMSnapshotVolumeVO> listSnapshotVolume = _vmSnapshotVolumeDao
                    .findByVMSnapshot(vmSnapshotId);
            List<VMSnapshotVolumeTO> listSnapshotVolumeTo = new ArrayList<VMSnapshotVolumeTO>();
            for (VMSnapshotVolumeVO snapshotVolume : listSnapshotVolume) {
                VolumeVO volume = _volumeDao.findById(snapshotVolume
                        .getSnapshotOf());
                VMSnapshotVolumeTO vTo = new VMSnapshotVolumeTO(
                        snapshotVolume.getId(), volume.getPath(),
                        snapshotVolume.getVolumePath());
                listSnapshotVolumeTo.add(vTo);
            }
            DeleteVMSnapshotCommand ccmd = new DeleteVMSnapshotCommand(
                    vmInstanceVo.getInstanceName(), vmSnapshot.getName(),
                    snapshotUUID, listSnapshotVolumeTo);
            DeleteVMSnapshotAnswer answer = (DeleteVMSnapshotAnswer) sendToPool(
                    hostId, ccmd);
            if (answer.getResult()) {
                processAnswer(vmSnapshot, answer);
            } else {
                s_logger.error("delete vm snapshot " + vmSnapshotId + " of vm "
                        + vmId + " failed due to " + answer.getDetails());
                return false;
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            s_logger.error("delete vm snapshot " + vmSnapshotId + " of vm "
                    + vmId + " failed due to " + msg);
            return false;
        }
        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VM_SNAPSHOT_REVERT, eventDescription = "revert_vm", async = true)
    public UserVm revertToSnapshot(RevertToSnapshotCmd cmd) {
        // TODO: timeout
        Long vmId = cmd.getVmId();
        Long vmSnapshotId = cmd.getVmSnapShotId();

        Account caller = UserContext.current().getCaller();

        // VMInstanceVO vmInstanceVo = _vmInstanceDao.findById(vmId);
        UserVmVO userVm = _userVMDao.findById(vmId);

        if (userVm == null) {
            throw new InvalidParameterValueException("Revert vm to snapshot: "
                    + vmSnapshotId + " failed due to vm: " + vmId
                    + " is not found");
        }

        VMSnapshotVO vmSnapshotVo = _vmSnapshotDao.findById(vmSnapshotId);
        if (vmSnapshotVo == null) {
            throw new InvalidParameterValueException(
                    "unable to find the vm snapshot with id " + vmSnapshotId);
        }

        if (userVm.getState() != VirtualMachine.State.Running
                && userVm.getState() != VirtualMachine.State.Stopped) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm is not in the state of Running or Stopped.");
        }

        if (vmSnapshotVo.getState() != VMSnapshot.Status.Created) {
            throw new InvalidParameterValueException(
                    "VM Snapshot reverting failed due to vm snapshot is not in the state of Created.");
        }

        _accountMgr.checkAccess(caller, null, true, vmSnapshotVo);

        List<VMSnapshotVolumeVO> listvsVolumes = _vmSnapshotVolumeDao
                .findByVMSnapshot(vmSnapshotId);
        List<VMSnapshotVolumeTO> listVolumeTo = new ArrayList<VMSnapshotVolumeTO>();

        Long hostId = pickRunningHost(vmId);
        boolean reverted = false;

        try {
            stateTransitTo(userVm, Event.RevertingRequested, hostId, null);

            VMSnapshotVO snapshot = _vmSnapshotDao.findById(vmSnapshotId);
            for (VMSnapshotVolumeVO snapshotVolume : listvsVolumes) {
                VolumeVO volume = _volumeDao.findById(snapshotVolume
                        .getSnapshotOf());
                VMSnapshotVolumeTO volumeTo = new VMSnapshotVolumeTO(
                        snapshotVolume.getId(), volume.getPath(),
                        snapshotVolume.getVolumePath(), volume.getVolumeType(),
                        volume.getDeviceId());
                listVolumeTo.add(volumeTo);
            }
            String vmInstanceName = userVm.getInstanceName();
            String vmSnapshotName = vmSnapshotVo.getName();
            RevertToSnapshotCommand ccmd = new RevertToSnapshotCommand(
                    vmInstanceName, vmSnapshotName,
                    vmSnapshotVo.getSnapshotUUID(), listVolumeTo,
                    snapshot.memory());
            RevertToSnapshotAnswer answer = (RevertToSnapshotAnswer) sendToPool(
                    hostId, ccmd);
            if (answer.getResult()) {
                processAnswer(vmSnapshotVo, answer);
                reverted = true;
                stateTransitTo(userVm, Event.OperationSucceeded, hostId, null);
            } else {
                reverted = false;
            }
        } catch (Exception e) {
            s_logger.error("revert vm: " + vmId + " to snapshot "
                    + vmSnapshotId + " failed due to " + e.getMessage(), e);
            reverted = false;
        } finally {
            if (!reverted) {
                try {
                    s_logger.error("Revert VM: " + vmId + " to snapshot:"
                            + vmSnapshotId + "failed. Try to stop vm.");
                    StopCommand scmd = new StopCommand(userVm.getInstanceName());
                    StopAnswer sanswer = (StopAnswer) sendToPool(hostId, scmd);
                    if (sanswer == null || !sanswer.getResult()) {
                        s_logger.warn("Unable to stop "
                                + userVm
                                + " due to "
                                + (sanswer != null ? sanswer.getDetails()
                                        : "no answers"));
                        _haMgr.scheduleStop(userVm, hostId, WorkType.ForceStop);
                        throw new ExecutionException(
                                "Unable to stop "
                                        + userVm
                                        + "so we are unable to retry the start operation ");
                    }
                    stateTransitTo(userVm, Event.OperationFailed, hostId, null);
                } catch (Exception e) {
                    s_logger.error(
                            "failed to stop vm after reverting failed due to "
                                    + e.getMessage(), e);
                }
            }
        }
        return userVm;
    }

    protected boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e,
            Long hostId, String reservationId) throws NoTransitionException {
        vm.setReservationId(reservationId);
        return _stateMachine.transitTo(vm, e,
                new Pair<Long, Long>(vm.getHostId(), hostId), _vmInstanceDao);
    }

    @Override
    public VMSnapshot getVMSnapshotById(long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        return vmSnapshot;
    }

    public String getChecksum(Long hostId, String templatePath) {
        HostVO ssHost = _hostDao.findById(hostId);
        Host.Type type = ssHost.getType();
        if (type != Host.Type.SecondaryStorage
                && type != Host.Type.LocalSecondaryStorage) {
            return null;
        }
        String secUrl = ssHost.getStorageUrl();
        Answer answer;
        answer = _agentMgr.sendToSecStorage(ssHost, new ComputeChecksumCommand(
                secUrl, templatePath));
        if (answer != null && answer.getResult()) {
            return answer.getDetails();
        }
        return null;
    }

    private Long pickRunningHost(Long vmId) {
        List<VolumeVO> listVolumes = _volumeDao.findByInstance(vmId);
        if (listVolumes == null || listVolumes.size() == 0) {
            throw new InvalidParameterValueException(
                    "vmInstance has no volumes");
        }
        VolumeVO volume = listVolumes.get(0);
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            throw new InvalidParameterValueException("pool id is not found");
        }
        StoragePoolVO storagePool = _storagePoolDao.findById(poolId);
        if (storagePool == null) {
            throw new InvalidParameterValueException(
                    "storage pool is not found");
        }
        List<HostVO> listHost = _hostDao.listAllUpAndEnabledNonHAHosts(
                Host.Type.Routing, storagePool.getClusterId(),
                storagePool.getPodId(), storagePool.getDataCenterId(), null);
        if (listHost == null || listHost.size() == 0) {
            throw new InvalidParameterValueException(
                    "no host in up state is found");
        }
        return listHost.get(0).getId();
    }

    @Override
    public VirtualMachine getVMBySnapshotId(Long id) {
        VMSnapshotVO vmSnapshot = _vmSnapshotDao.findById(id);
        Long vmId = vmSnapshot.getvmId();
        UserVmVO vm = _vmDao.findById(vmId);
        return vm;
    }

    @Override
    public boolean deleteAllVMSnapshots(long vmId) {
        boolean result = true;
        List<VMSnapshotVO> listVmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (listVmSnapshots == null || listVmSnapshots.isEmpty()) {
            return true;
        }
        for (VMSnapshotVO snapshot : listVmSnapshots) {
            if (!deleteSnapshotInternal(vmId, snapshot.getId())) {
                result = false;
            }
        }
        return result;
    }

    protected class VMSnapshotGarbageCollector implements Runnable {

        public VMSnapshotGarbageCollector() {
        }

        @Override
        public void run() {
            try {
                s_logger.trace("VM Snapshot Garbage Collection Thread is running.");

                cleanupVMSnapshot(true);

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    public void cleanupVMSnapshot(boolean recurring) {
        s_logger.debug("cleaning up expunging vm snapshot");
        GlobalLock scanLock = GlobalLock.getInternLock("vmsnapshotmgr.cleanup");

        try {
            if (scanLock.lock(3)) {
                try {
                    List<VMSnapshotVO> expungingVMSnapshots = _vmSnapshotDao
                            .listExpungingSnapshot();
                    for (VMSnapshotVO vmSnapshot : expungingVMSnapshots) {
                        Long vmId = vmSnapshot.getvmId();
                        Long vmSnapshotId = vmSnapshot.getId();
                        if (!deleteSnapshotInternal(vmId, vmSnapshotId)) {
                            s_logger.error("delete snapshot: " + vmSnapshotId
                                    + " of vm: " + vmId + " failed");
                        }
                    }
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    public void syncVolumePath(Long hostId) {
        s_logger.warn("begin to sync volume path");
        HostVO hostVo = _hostDao.findById(hostId);
        ClusterVO clusterVo = _clusterDao.findById(hostVo.getClusterId());
        List<VMInstanceVO> vmList = _vmInstanceDao.listLHByClusterId(clusterVo
                .getId());
        Map<String, Map<String, VolumeTO>> vmVolumesMap = new HashMap<String, Map<String, VolumeTO>>();
        for (VMInstanceVO vm : vmList) {
            if (VirtualMachine.State.Destroyed != vm.getState()
                    && VirtualMachine.State.Error != vm.getState()
                    && VirtualMachine.State.Expunging != vm.getState()) {
                List<VolumeVO> volumeList = _volumeDao.findByInstance(vm
                        .getId());
                Map<String, VolumeTO> volumeMap = new HashMap<String, VolumeTO>();
                for (VolumeVO volume : volumeList) {
                    StoragePoolVO storagePool = _storagePoolDao.findById(volume
                            .getPoolId());
                    String key = null;
                    if (vm.getHypervisorType() == HypervisorType.XenServer) {
                        key = volume.getDeviceId().toString();
                    } else if (vm.getHypervisorType() == HypervisorType.VMware) {
                        key = volume.getName();
                    }
                    volumeMap.put(key, new VolumeTO(volume, storagePool));
                }
                vmVolumesMap.put(vm.getInstanceName(), volumeMap);
            }
        }
        try {
            GetVolumesChangedCommand ccmd = new GetVolumesChangedCommand(
                    vmVolumesMap);
            GetVolumesChangedAnswer answer = (GetVolumesChangedAnswer) sendToPool(
                    hostId, ccmd);
            if (answer.getResult()) {
                List<VolumeTO> resultVolumeToList = answer.getVmVolumesMap();
                for (VolumeTO volumeTo : resultVolumeToList) {
                    VolumeVO volumeVo = _volumeDao.findById(volumeTo.getId());
                    volumeVo.setPath(volumeTo.getPath());
                    _volumeDao.persist(volumeVo);
                }
            }
        } catch (AgentUnavailableException e) {
            String msg = e.getMessage();
            s_logger.error("sync volume path failed due to " + msg);
        } catch (OperationTimedoutException e) {
            String msg = e.getMessage();
            s_logger.error("sync volume path failed due to " + msg);
            ;
        }
    }
}
