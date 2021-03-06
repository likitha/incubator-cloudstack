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
package org.apache.cloudstack.api.response;

import com.cloud.dc.Vlan;
import org.apache.cloudstack.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value=Vlan.class)
@SuppressWarnings("unused")
public class VlanIpRangeResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the VLAN IP range")
    private String id;

    @SerializedName("forvirtualnetwork") @Param(description="the virtual network for the VLAN IP range")
    private Boolean forVirtualNetwork;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID of the VLAN IP range")
    private String zoneId;

    @SerializedName(ApiConstants.VLAN) @Param(description="the ID or VID of the VLAN.")
    private String vlan;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account of the VLAN IP range")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID of the VLAN IP range")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the VLAN IP range")
    private String domainName;

    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID for the VLAN IP range")
    private String podId;

    @SerializedName("podname") @Param(description="the Pod name for the VLAN IP range")
    private String podName;

    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway of the VLAN IP range")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the VLAN IP range")
    private String netmask;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="the description of the VLAN IP range")
    private String description;

    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the VLAN IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the VLAN IP range")
    private String endIp;

    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the network id of vlan range")
    private String networkId;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vlan range")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vlan range")
    private String projectName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network this belongs to")
    private String physicalNetworkId;

    public void setId(String id) {
        this.id = id;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getphysicalNetworkId() {
        return physicalNetworkId;
    }
}
