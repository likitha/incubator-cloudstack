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
(function($, cloudStack) {
  cloudStack.sections.vmsnapshots = {
    title: 'label.vmsnapshots',
    id: 'vmsnapshots',
    listView: {
      id: 'vmsnapshots',
      isMaximized: true,
      fields: {
        displayname: {
          label: 'label.name'
        },
        created: {
          label: 'label.date',
          converter: cloudStack.converters.toLocalDate
        },
        state: {
          label: 'label.state',
          indicator: {
            'Created': 'on',
            'Error': 'off'
          }
        }
      },

      dataProvider: function(args) {
        var apiCmd = "listVMSnapshot&listAll=true";
        if (args.context != null) {
          if ("instances" in args.context) {
            apiCmd += "&vmid=" + args.context.instances[0].id;
          }
        }
        $.ajax({
          url: createURL(apiCmd),
          dataType: "json",
          async: true,
          success: function(json) {
            var jsonObj;
            jsonObj = json.listvmsnapshotresponse.vmSnapshot;
            args.response.success({
              //actionFilter: vmActionfilter,
              data: jsonObj
            });
          }
        });
      },
      //dataProvider end
      detailView: {
        tabs: {
          details: {
            title: 'label.details',
            fields: {
              id: {
                label: 'label.id'
              },
              name: {
                label: 'label.name'
              },
              displayname: {
                label: 'label.display.name',
                isEditable: true
              },
              description: {
                label: 'label.description',
                isEditable: true
              },
              created: {
                label: 'label.date',
                converter: cloudStack.converters.toLocalDate
              },
              state: {
                label: 'label.state',
                indicator: {
                  'Created': 'on',
                  'Error': 'off'
                }
              }
            },
            dataProvider: function(args) {
              $.ajax({
                url: createURL("listVMSnapshot&id=" + args.context.vmsnapshots[0].id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jsonObj;
                  jsonObj = json.listvmsnapshotresponse.vmSnapshot[0];
                  args.response.success({
                    //actionFilter: vmActionfilter,
                    data: jsonObj
                  });
                }
              });
            },
            //dateProvider end 
          }
        },
        actions: {
          //delete a snapshot
          destroy: {
            label: 'label.action.delete.snapshot',
            messages: {
              confirm: function(args) {
                return 'message.action.delete.snapshot';
              },
              notification: function(args) {
                return 'label.action.delete.snapshot';
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("deleteVMSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.deletevmsnapshotresponse.success;
                  args.response.success({
                    _custom: {
                      jobId: jid
                      //        getUpdatedItem: function(json) {
                      //            return json.queryasyncjobresultresponse.jobresult.virtualmachine;
                      //          }
                    }
                  });

                }
              });
            },
            notification: {
              poll: function(args) {
                args.complete();
              }
            }
          },
          restart: {
            label: 'label.action.vmsnapshot.revert',
            messages: {
              confirm: function(args) {
                return 'label.action.vmsnapshot.revert';
              },
              notification: function(args) {
                return 'message.vmsnapshot.revert';
              }
            },
            action: function(args) {
              $.ajax({
                url: createURL("revertToSnapshot&vmsnapshotid=" + args.context.vmsnapshots[0].id),
                dataType: "json",
                async: true,
                success: function(json) {
                  var jid = json.reverttosnapshotresponse.jobid;
                  args.response.success({
                    _custom: {
                      jobId: jid
                      //          getUpdatedItem: function(json) {
                      //            return json.queryasyncjobresultresponse.jobresult;
                      //          },
                      //          getActionFilter: function() {
                      //            return vmActionfilter;
                      //          }
                    }
                  });
                }
              });

            },
            notification: {
              poll: function(args) {
                args.complete();
              }
            }
          }
        }
      }
      //detailview end
    }
  }
})(jQuery, cloudStack);