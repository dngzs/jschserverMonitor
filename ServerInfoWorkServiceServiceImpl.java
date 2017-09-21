package com.zjlp.face.monitor.service.impl;

import com.zjlp.face.monitor.entity.dto.ServerInfoDto;
import com.zjlp.face.monitor.result.PageResultForBootstrap;
import com.zjlp.face.monitor.service.LinuxStateForShellService;
import com.zjlp.face.monitor.service.ServerInfoWorkService;
import com.zjlp.face.sysmanager.dto.GridData;
import com.zjlp.face.sysmanager.model.ServerData;
import com.zjlp.face.sysmanager.model.ServiceData;
import com.zjlp.face.sysmanager.service.ServerService;
import com.zjlp.face.sysmanager.service.ServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhangbo 2017年09月18日
 * @reviewer
 */
@Service
public class ServerInfoWorkServiceServiceImpl implements ServerInfoWorkService{
    private static  final Logger LOGGER = LoggerFactory.getLogger(ServerInfoWorkServiceServiceImpl.class);

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private LinuxStateForShellService linuxStateForShellService;


    @Override
    public List<ServerInfoDto>  getServerInfoWork() {
        List<ServerInfoDto> serverInfoDtos = new ArrayList<>();
        List<ServiceData> serviceDatas = serviceService.getServiceInfoAll();
        if(serviceDatas != null && serviceDatas.size()>0){
            for(int i=0;i<serviceDatas.size();i++){
                ServerInfoDto serverInfoDto = new ServerInfoDto();
                ServiceData serviceData = serviceDatas.get(i);
                if(serviceData!= null && serviceData.getServerId()>0){
                    int serverId = serviceData.getServerId();
                    //获取服务器信息
                    ServerData serverData = serverService.getServerInfoById(serverId);
                    String ip = serverData.getServerAddress();
                    serverInfoDto.setIp(ip);
                    serverInfoDto.setId(serviceData.getServiceId()+"_"+new Integer(serverData.getServerId()).toString());
                    serverInfoDto.setProcessName(serviceData.getServiceName());
                    Map<String, String> logChange = linuxStateForShellService.getLogChange(serviceData.getServiceName(),
                            serverData.getOsUsername(),serverData.getOsPassword(),serverData.getServerAddress(), Integer.valueOf(serverData.getSshdPort()),serverData.getPath());
                    Map<String, Object> projecProcess = linuxStateForShellService.getProjecProcess(serviceData.getServiceName(),
                            serverData.getOsUsername(), serverData.getOsPassword(), serverData.getServerAddress(), Integer.valueOf(serverData.getSshdPort()));
                    serverInfoDto.setStatus((Boolean)projecProcess.get("success")?1:0);
                    String s = (String)logChange.get("info.log");
                    String date = serviceDatas.get(i).getLogChangeDate();
                    if(s.equals(date)){
                       //未变更
                        serverInfoDto.setLogStatus(1);
                    }else{
                        //更新到数据库
                        //时间变更
                        serviceService.updateServiceDateInfoById(serviceData.getServiceId(),s);
                        serverInfoDto.setLogStatus(0);
                    }
                    serverInfoDtos.add(serverInfoDto);
                }
            }
            return serverInfoDtos;
        }else{
            return null;
        }

    }
}
