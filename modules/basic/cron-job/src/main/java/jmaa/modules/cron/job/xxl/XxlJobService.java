package jmaa.modules.cron.job.xxl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.tools.SpringUtils;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class XxlJobService {
    private final Map<String, String> loginCookie = new HashMap<>();
    private static XxlJobService instance;

    public static XxlJobService getService() {
        if (instance == null) {
            instance = new XxlJobService();
            instance.xxlJobConfig = SpringUtils.getBean(XxlJobConfig.class);
        }
        return instance;
    }

    public static void setService(XxlJobService service) {
        instance = service;
    }

    XxlJobConfig xxlJobConfig;

    public XxlJobConfig getXxlJobConfig() {
        return xxlJobConfig;
    }

    /**
     * 登录xxlJob
     */
    void xxlJobLogin() {
        String url = getXxlJobConfig().getAddress() + "/login";
        HttpResponse response = HttpRequest.post(url)
            .form("userName", getXxlJobConfig().getUserName())
            .form("password", getXxlJobConfig().getPassword())
            .form("accessToken", getXxlJobConfig().getExecutor().getAccessToken())
            .execute();
        List<HttpCookie> cookies = response.getCookies();
        Optional<HttpCookie> cookieOpt = cookies.stream()
            .filter(cookie -> "XXL_JOB_LOGIN_IDENTITY".equals(cookie.getName())).findFirst();
        if (cookieOpt.isPresent()) {
            String value = cookieOpt.get().getValue();
            loginCookie.put("XXL_JOB_LOGIN_IDENTITY", value);
        }
    }

    /**
     * 获取xxlJob的Cookie
     */
    public String getXxlJobCookie() {
        for (int i = 0; i < 3; i++) {
            String cookieStr = loginCookie.get("XXL_JOB_LOGIN_IDENTITY");
            if (cookieStr != null) {
                return "XXL_JOB_LOGIN_IDENTITY=" + cookieStr;
            }
            xxlJobLogin();
        }
        throw new PlatformException("xxl-job登录失败");
    }

    public XxlJobInfo getJobInfo(String jobId) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/pageList";
        HttpResponse response = HttpRequest.post(url)
            .form("jobGroup", getXxlJobConfig().getJobGroup())
            .form("author", jobId)
            .form("triggerStatus", -1)
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object data = json.getByPath("data");
        if (data instanceof JSONArray) {
            JSONArray array = (JSONArray) data;
            if (array.size() > 0) {
                return JSONUtil.toBean((JSONObject) array.get(0), XxlJobInfo.class);
            }
            return null;
        }
        throw new PlatformException("xxl-job查询失败:" + json.getByPath("msg"));
    }

    /**
     * 注册一个新任务，最终返回创建的新任务的id
     */
    public Integer addJobInfo(XxlJobInfo xxlJobInfo) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/add";
        xxlJobInfo.setId(null);
        xxlJobInfo.setJobGroup(getXxlJobConfig().getJobGroup());
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setGlueType("BEAN");
        String policy = getXxlJobConfig().getExecutor().getRoutingPolicy();
        xxlJobInfo.setExecutorRouteStrategy(Utils.isBlank(policy) ? "ROUND" : policy);
        Map<String, Object> paramMap = BeanUtil.beanToMap(xxlJobInfo);
        HttpResponse response = HttpRequest.post(url)
            .form(paramMap)
            .cookie(getXxlJobCookie())
            .execute();

        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (code.equals(200)) {
            return Utils.toInt(json.getByPath("content"));
        }
        throw new PlatformException("xxl-job添加失败：" + json.getByPath("msg"));
    }

    /**
     * 修改任务
     *
     * @param xxlJobInfo
     */
    public void updateJobInfo(XxlJobInfo xxlJobInfo) {
        xxlJobInfo.setAddTime(null);
        xxlJobInfo.setUpdateTime(null);
        xxlJobInfo.setGlueUpdatetime(null);
        String url = getXxlJobConfig().getAddress() + "/jobinfo/update";
        Map<String, Object> paramMap = BeanUtil.beanToMap(xxlJobInfo);
        HttpResponse response = HttpRequest.post(url)
            .form(paramMap)
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (!code.equals(200)) {
            throw new PlatformException("xxl-job更新失败：" + json.getByPath("msg"));
        }
    }

    /**
     * 删除任务
     */
    public void deleteJob(int jobId) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/remove";
        HttpResponse response = HttpRequest.post(url)
            .form("id", jobId)
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (!code.equals(200)) {
            throw new PlatformException("xxl-job删除失败：" + json.getByPath("msg"));
        }
    }

    /**
     * 执行一次
     */
    public void trigger(int jobId, Map<String, Object> param) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/trigger";
        HttpResponse response = HttpRequest.post(url)
            .form("id", jobId)
            .form("executorParam", com.alibaba.fastjson.JSON.toJSONString(param))
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (!code.equals(200)) {
            throw new PlatformException("xxl-job执行失败：" + json.getByPath("msg"));
        }
    }


    /**
     * 停止任务
     */
    public void stopJob(int jobId) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/stop";
        HttpResponse response = HttpRequest.post(url)
            .form("id", jobId)
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (!code.equals(200)) {
            throw new PlatformException("xxl-job停止失败：" + json.getByPath("msg"));
        }
    }

    /**
     * 开始任务
     */
    public void startJob(int jobId) {
        String url = getXxlJobConfig().getAddress() + "/jobinfo/start";
        HttpResponse response = HttpRequest.post(url)
            .form("id", jobId)
            .cookie(getXxlJobCookie())
            .execute();
        JSON json = JSONUtil.parse(response.body());
        Object code = json.getByPath("code");
        if (!code.equals(200)) {
            throw new PlatformException("xxl-job启动失败：" + json.getByPath("msg"));
        }
    }
}
