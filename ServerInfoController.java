package com.zjlp.face.monitor.ctl;

import com.zjlp.face.monitor.entity.dto.ServerInfoDto;
import com.zjlp.face.monitor.result.PageResultForBootstrap;
import com.zjlp.face.monitor.service.LinuxStateForShellService;
import com.zjlp.face.monitor.service.ServerInfoWorkService;
import com.zjlp.face.sysmanager.model.ServerData;
import com.zjlp.face.sysmanager.model.ServiceData;
import com.zjlp.face.sysmanager.service.ServerService;
import com.zjlp.face.sysmanager.service.ServiceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangbo 2017年09月13日
 * @reviewer
 */
@Controller
@RequestMapping("serverInfo")
public class ServerInfoController {
    @Autowired
    private LinuxStateForShellService linuxStateForShellService;
    @Autowired
    private ServerInfoWorkService serverInfoWorkService;

    public static final String FILES_SHELL = "df -hl";

    public static final String CPU_MEM_SHELL = "top -b -n 1";

    public static final String[] COMMANDS = {CPU_MEM_SHELL, FILES_SHELL};

    @Autowired
    private ServerService serverService;

    @Autowired
    private ServiceService serviceService;

    @ResponseBody
    @RequestMapping("hardwareInfo")
    public Map getServerInfo(String id){
        Map<String, String> result = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> info = getInfo(id);
        if(info != null){
            result = linuxStateForShellService.runDistanceExec(COMMANDS, (String)info.get("user"),(String)info.get("password"), (String)info.get("host"),
                    Integer.valueOf((String) info.get("port")));
            Map<String, String> stringStringMap = linuxStateForShellService.disposeResultMessage(result, "/");
            return stringStringMap;
        }else{
            map.put("success",false);
            map.put("msg", "服务器错误");
            return map;
        }

    }


    @ResponseBody
    @RequestMapping("project")
    public Map getprojectInfo(@RequestParam(required = true)  String id){
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> info = getInfo(id);
        if(info != null){
           result = linuxStateForShellService.getProjecProcess((String) info.get("projectName"),(String)info.get("user"),(String)info.get("password"), (String)info.get("host"),
                   Integer.valueOf((String) info.get("port")));
        }else{
            result.put("success", false);
            result.put("msg", "服务器错误");
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("getServerInfoWork")
    public List<ServerInfoDto> getServerInfoWork(){
        List<ServerInfoDto> serverInfoWork = serverInfoWorkService.getServerInfoWork();
        return serverInfoWork;
    }

    private Map<String,Object> getInfo(String id){
        String serverId = null;
        String serviceId = null;
        Map<String,Object> map = new HashMap<>();
        if(StringUtils.isNotBlank(id)){
            serverId = id.split("_")[1];
            serviceId = id.split("_")[0];
        }else{
            return  null;
        }
        ServerData serverData = serverService.getServerInfoById(Integer.valueOf(serverId));
        ServiceData serviceData = serviceService.getServiceInfoById(Integer.valueOf(serviceId));
        map.put("projectName",serviceData.getServiceName());
        map.put("user",serverData.getOsUsername());
        map.put("password",serverData.getOsPassword());
        map.put("host",serverData.getServerAddress());
        map.put("port",serverData.getSshdPort());
        map.put("path",serverData.getPath());
        return map;
    }
}
