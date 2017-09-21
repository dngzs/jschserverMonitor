package com.zjlp.face.monitor.service.impl;

import com.jcraft.jsch.*;
import com.zjlp.face.monitor.entity.dto.ShellChannelContainer;
import com.zjlp.face.monitor.service.LinuxStateForShellService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @author zhangbo 2017年09月12日
 * @reviewer
 */
@Service
public class LinuxStateForShellServiceImpl implements LinuxStateForShellService {


    private static final Logger LOGGER = LoggerFactory.getLogger(LinuxStateForShellServiceImpl.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static final String FILES_SHELL = "df -hl";

    public static final String CPU_MEM_SHELL = "top -b -n 1";

    public static final String[] COMMANDS = {CPU_MEM_SHELL, FILES_SHELL};


    private Channel shellConnect(String user, String pwd, String host, Integer port, String channelType, String sessionId) {
        String key = sessionId + "_" + user + host + port;
        if (ShellChannelContainer.containsKey(key)) {
            Channel channel = ShellChannelContainer.getChannel(key);
            return channel;
        } else {
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, host, port);
                session.setPassword(pwd);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                Channel channel = session.openChannel(channelType);
                channel.connect(60000);
                ShellChannelContainer.putChannel(key, channel);
                return channel;
            } catch (JSchException e) {
                LOGGER.error("【IM管理】ssh服务{}连接到远程系统失败", host, e);
                return null;
            }
        }
    }


    @Override
    public Map<String, String> disposeResultMessage(Map<String, String> result, String diskName) {
        StringBuilder buffer = new StringBuilder();
        Map<String, String> resMap = new HashMap<String, String>();
        for (String command : COMMANDS) {
            String commandResult = result.get(command);
            if (null == commandResult) continue;

            if (command.equals(CPU_MEM_SHELL)) {
                String[] strings = commandResult.split(LINE_SEPARATOR);
                //将返回结果按换行符分割
                for (String line : strings) {
                    line = line.toUpperCase();//转大写处理
                    //处理CPU Cpu(s): 10.8%us,  0.9%sy,  0.0%ni, 87.6%id,  0.7%wa,  0.0%hi,  0.0%si,  0.0%st
                    if (line.startsWith("CPU(S):")) {
                        String cpuStr = "CPU 用户使用占有率:";
                        try {
                            cpuStr += line.split(":")[1].split(",")[0].replace("US", "");
                            resMap.put("cupRate", line.split(":")[1].split(",")[0].replace("US", ""));
                        } catch (Exception e) {
                            e.printStackTrace();
                            cpuStr += "计算过程出错";
                        }
                        buffer.append(cpuStr).append(LINE_SEPARATOR);
                        //处理内存 Mem:  66100704k total, 65323404k used,   777300k free,    89940k buffers
                    } else if (line.startsWith("MEM")) {
                        String memStr = "内存使用情况:";
                        try {
                            memStr += line.split(":")[1]
                                    .replace("TOTAL", "总计")
                                    .replace("USED", "已使用")
                                    .replace("FREE", "空闲")
                                    .replace("BUFFERS", "缓存");
                            String[] mems = line.split(":")[1].split(",");
                            String total = mems[0].replace("TOTAL", "").trim();
                            String used = mems[1].replace("USED", "").trim();
                            int memTotal = Integer.parseInt(total.substring(0, total.length() - 1));
                            int memUsed = Integer.parseInt(used.substring(0, used.length() - 1));
                            int memRate = memUsed * 100 / memTotal;
                            resMap.put("memTotal", total);
                            resMap.put("memUsed", used);
                            resMap.put("memRate", memRate + "%");

                        } catch (Exception e) {
                            e.printStackTrace();
                            memStr += "计算过程出错";
                            buffer.append(memStr).append(LINE_SEPARATOR);
                            continue;
                        }
                        buffer.append(memStr).append(LINE_SEPARATOR);

                    }
                }
            } else if (command.equals(FILES_SHELL)) {
                //处理系统磁盘状态
                buffer.append("系统磁盘状态:");
                try {
                    List<String> strs = disposeFilesSystem(commandResult, diskName);
                    resMap.put("diskTotal", strs.get(0));
                    resMap.put("diskUsed", strs.get(1));
                    resMap.put("diskRate", strs.get(2));
                } catch (Exception e) {
                    e.printStackTrace();
                    buffer.append("计算过程出错").append(LINE_SEPARATOR);
                }
            }
        }
        return resMap;
    }


    /**
     * 处理系统磁盘状态
     * <p>
     * Filesystem            Size  Used Avail Use% Mounted on
     * /dev/sda3             442G  327G   93G  78% /
     * tmpfs                  32G     0   32G   0% /dev/shm
     * /dev/sda1             788M   60M  689M   8% /boot
     * /dev/md0              1.9T  483G  1.4T  26% /ezsonar
     *
     * @param commandResult 处理系统磁盘状态shell执行结果
     * @return 处理后的结果
     * @author zhangbo 2017年9月14日14:04:50
     */
    public static List<String> disposeFilesSystem(String commandResult, String diskName) {
        String[] strings = commandResult.split(LINE_SEPARATOR);
        // final String PATTERN_TEMPLATE = "([a-zA-Z0-9%_/]*)\\s";
        String size = "";
        String used = "";
        String rate = "";
        for (int i = 0; i <= strings.length - 1; i++) {
            if (i == 0) continue;
            int temp = 0;
            String[] lineStrsBefore = strings[i].split(" ");
            if (lineStrsBefore.length == 1) continue;
            String lineStr = "";
            for (String s : lineStrsBefore) {
                if (!s.equals("") && s.length() > 0) {
                    if (lineStr.equals("")) {
                        lineStr += s;
                    } else {
                        lineStr += "," + s;
                    }
                }
            }
            String[] lineStrs = lineStr.split(",");
            if (lineStrs[lineStrs.length - 1].equals(diskName)) {
                rate = lineStrs[lineStrs.length - 2];
                used = lineStrs[lineStrs.length - 4];
                size = lineStrs[lineStrs.length - 5];
            }
        }
        List<String> list = new ArrayList<String>();
        list.add(size);
        list.add(used);
        list.add(rate);
        return list;
    }


    @Override
    public InputStream runDistanceShell(String projectName, String user, String pwd, String host,
                                        Integer port, String sessionId,String path) {
        Channel channel = null;
        DataInputStream dataIn = null;
        DataOutputStream dataOut = null;
        try {
            channel = shellConnect(user, pwd, host, port, "shell", sessionId);
            if (channel != null) {
                dataOut = new DataOutputStream(channel.getOutputStream());
                dataOut.writeBytes("tail -100f "+path+"/"+projectName+"/info.log" + LINE_SEPARATOR);
                dataOut.flush();
                StringBuffer stringBuffer = new StringBuffer();
                return channel.getInputStream();
            } else {
                throw new Exception("获取channel失败，值为{}");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), projectName, e);
            return null;
        } finally {
            try {
                if (dataOut != null) {
                    dataOut.close();
                }
            } catch (IOException e) {
                LOGGER.error("【IM管理】关闭IO流失败", e);
            }
        }
    }


    /**
     * exec linux链接
     *
     * @param user
     * @param pwd
     * @param host
     * @param port
     * @return Map
     * @author zhangbo 2017年9月14日14:03:40
     */
    private Map execCollect(String user, String pwd, String host, Integer port) {
        Map map = new HashMap();
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(pwd);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("exec");
            map.put("session", session);
            map.put("channel", channel);
            return map;
        } catch (JSchException e) {
            LOGGER.error("【IM管理】ssh服务{}连接到远程系统失败", host, e);
            return null;
        }
    }


    @Override
    public Map<String, String> runDistanceExec(String[] commands, String user, String pwd,
                                               String host, Integer port) {
        Map<String, String> map = new HashMap<String, String>();
        StringBuilder stringBuilder;
        BufferedReader reader = null;
        Channel channel = null;
        Session session = null;
        try {
            for (String command : commands) {
                stringBuilder = new StringBuilder();
                Map collection = execCollect(user, pwd, host, port);
                if (collection != null) {
                    session = (Session) collection.get("session");
                    channel = (Channel) collection.get("channel");
                }
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                channel.connect();
                InputStream in = channel.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                String buf;
                while ((buf = reader.readLine()) != null) {
                    //舍弃PID 进程信息
                    if (buf.contains("PID")) {
                        break;
                    }
                    stringBuilder.append(buf.trim()).append(LINE_SEPARATOR);
                }
                //每个命令存储自己返回数据-用于后续对返回数据进行处理
                map.put(command, stringBuilder.toString());
            }
        } catch (IOException | JSchException e) {
            LOGGER.error("【IM管理】执行{}远程命令失败", commands, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                LOGGER.error("【IM管理】关闭IO流失败", e);
            }
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return map;
    }


    @Override
    public Map<String, Object> getProjecProcess(String projectName,String user,String pwd,String host,Integer prot) {
        Map<String, Object> map = new HashMap<>();
        Map<String, String> result = this.runDistanceExec(new String[]{"ps -ef|grep " + projectName}, user, pwd, host, prot);
        String s = result.get("ps -ef|grep " + projectName);
        String[] split = s.split(LINE_SEPARATOR);
        for (int $i = 0; $i < split.length; $i++) {
            if (split[$i].endsWith("org.apache.catalina.startup.Bootstrap start") || (split[$i].indexOf("java -server") > -1 && split[$i].endsWith(".jar"))) {
                map.put("success", true);
                map.put("msg", projectName + "项目进程存在");
                return map;
            }
        }
        map.put("success", false);
        map.put("msg", projectName + "项目进程不存在");
        return map;
    }

    @Override
    public Map<String, String> getLogChange(String projectName,String user,String pwd,String host,Integer port,String path) {
        String com = "ls "+path+"/"+projectName+" --full-time";
        Map<String, String> result = this.runDistanceExec(new String[]{com}, user, pwd, host, port);
        String ss = result.get(com);
        String[] split = ss.split(System.getProperty("line.separator"));
        Map<String,String> map = new HashMap<>();
        for (int i =0; i <split.length; i++) {
            String[] split1 = split[i].split(" ");
            String key ="";
            String value ="";
            for(int j=split1.length;j >=0;j--){
                if(j == split1.length-1){
                    key = split1[j];
                }
                if(j == split1.length-3){
                    value = split1[j];
                    map.put(key,value.split("\\.")[0]);
                }
            }
        }
        return map;
    }
}
