package me.shumei.open.oks.vdisk;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.shumei.open.oks.vdisk.tools.HttpUtil;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			Document doc;
			
			String loginPageUrl = "http://vdisk.weibo.com/wap/account/login";
			String loginSubmitUrl = "";
			String ssoUrl = "";//单点登录URL
			String signinfoUrl = "http://vdisk.weibo.com/wap/api/weipan/2/checkin/checkin_info";//检查签到状态的URL
			String siginUrl = "http://vdisk.weibo.com/wap/api/weipan/2/checkin/checkin";
			String sendWeiBoUrl = "http://vdisk.weibo.com/wap/api/weipan/2/checkin/checkin_send_weibo";
			
			//HttpClient的数据
			String clientStrResult = "";
			CookieStore cookieStore = null;
			
			//HttpGet连接对象
			HttpGet httpGet = new HttpGet(loginPageUrl);
			//取得HttpClient对象
			HttpClient httpClient = HttpUtil.getNewHttpClient();
			//设置UA，超时等
			HttpProtocolParams.setUserAgent(httpClient.getParams(), UA_ANDROID);
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIME_OUT);
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIME_OUT);
			//请求HttpClient，取得HttpResponse
			HttpResponse httpResponse = httpClient.execute(httpGet);
			//请求成功
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				//取得返回的字符串
				clientStrResult = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
				cookieStore = ((AbstractHttpClient) httpClient).getCookieStore();
			}
			cookies.putAll(HttpUtil.CookieStroeToHashMap(cookieStore));
			
			//分析登录页面的数据
			doc = Jsoup.parse(clientStrResult);
			Element form = doc.select("form").first();
			String action = form.attr("action");
			String pwdFieldName = form.getElementsByAttributeValue("type", "password").attr("name");
			String backURL = form.getElementsByAttributeValue("name", "backURL").val();
			String vk = form.getElementsByAttributeValue("name", "vk").val();
			loginSubmitUrl = "https://login.weibo.cn/login/" + action;
			
			//提交登录信息
			List<NameValuePair> postDatas = new ArrayList<NameValuePair>();
			postDatas.add(new BasicNameValuePair("mobile", user));
			postDatas.add(new BasicNameValuePair(pwdFieldName, pwd));
			postDatas.add(new BasicNameValuePair("remember", ""));
			postDatas.add(new BasicNameValuePair("backURL", backURL));
			postDatas.add(new BasicNameValuePair("backTitle", "手机新浪网"));
			postDatas.add(new BasicNameValuePair("tryCount", ""));
			postDatas.add(new BasicNameValuePair("vk", vk));
			postDatas.add(new BasicNameValuePair("submit", "登录"));
			
			//<body><div class="s"></div><div class="c">登录成功!返回登录前的页面...</div><div class="c">如果没有自动跳转,请<a href="http://login.weibo.cn/login/setssocookie/?backUrl=http%3A%2F%2Fvdisk.weibo.com%2Fwap%2Faccount%2Flogin%3F&amp;loginpage=&amp;gsid=4KwkCpOz1ZWKCNseQLsya94lLfv&amp;PHPSESSID=&amp;vt=4">点击这里</a>.</div><div class="s"></div></body>
			HttpPost httpPost = new HttpPost(loginSubmitUrl);
			httpPost.setEntity(new UrlEncodedFormEntity(postDatas, "UTF-8"));
			httpClient = HttpUtil.getNewHttpClient();
			HttpProtocolParams.setUserAgent(httpClient.getParams(), UA_ANDROID);
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIME_OUT);
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIME_OUT);
			httpResponse = httpClient.execute(httpPost);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				clientStrResult = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
				cookieStore = ((AbstractHttpClient) httpClient).getCookieStore();
			}
			
			
			if (clientStrResult.contains("登录名或密码错误")) {
				return new String[]{"false", "登录名或密码错误"};
			} else if (clientStrResult.contains("登录次数过于频繁")) {
				return new String[]{"false", "登录次数过于频繁，"};
			}
			
			//判断是否需要输入验证码
			if (clientStrResult.contains("请输入验证码")) {
				doc = Jsoup.parse(clientStrResult);
				form = doc.select("form").first();
				action = form.attr("action");
				pwdFieldName = form.getElementsByAttributeValue("type", "password").attr("name");
				backURL = form.getElementsByAttributeValue("name", "backURL").val();
				vk = form.getElementsByAttributeValue("name", "vk").val();
				String captchaUrl = form.select("img").first().attr("src");
				String capId = form.getElementsByAttributeValue("name", "capId").val();
				loginSubmitUrl = "https://login.weibo.cn/login/" + action;
				
				if (CaptchaUtil.showCaptcha(captchaUrl, UA_ANDROID, cookies, "新浪微盘", user, "登录需要验证码")) {
					if (CaptchaUtil.captcha_input.length() > 0) {
						//获取验证码成功，可以用CaptchaUtil.captcha_input继续做其他事了
						//Don't need to do anything...
					} else {
						//用户取消输入验证码
						this.resultFlag = "false";
						this.resultStr = "用户放弃输入验证码，登录失败";
						return new String[]{this.resultFlag,this.resultStr};
					}
				} else {
					//拉取验证码失败，签到失败
					this.resultFlag = "false";
					this.resultStr = "拉取验证码失败，无法登录";
					return new String[]{this.resultFlag,this.resultStr};
				}
				
				//提交登录数据
				postDatas = new ArrayList<NameValuePair>();
				postDatas.add(new BasicNameValuePair("mobile", user));
				postDatas.add(new BasicNameValuePair(pwdFieldName, pwd));
				postDatas.add(new BasicNameValuePair("code", CaptchaUtil.captcha_input));
				postDatas.add(new BasicNameValuePair("remember", ""));
				postDatas.add(new BasicNameValuePair("backURL", backURL));
				postDatas.add(new BasicNameValuePair("backTitle", "手机新浪网"));
				postDatas.add(new BasicNameValuePair("tryCount", ""));
				postDatas.add(new BasicNameValuePair("vk", vk));
				postDatas.add(new BasicNameValuePair("capId", capId));
				postDatas.add(new BasicNameValuePair("submit", "登录"));
				
				//<body><div class="s"></div><div class="c">登录成功!返回登录前的页面...</div><div class="c">如果没有自动跳转,请<a href="http://login.weibo.cn/login/setssocookie/?backUrl=http%3A%2F%2Fvdisk.weibo.com%2Fwap%2Faccount%2Flogin%3F&amp;loginpage=&amp;gsid=4KwkCpOz1ZWKCNseQLsya94lLfv&amp;PHPSESSID=&amp;vt=4">点击这里</a>.</div><div class="s"></div></body>
				httpPost = new HttpPost(loginSubmitUrl);
				httpPost.setEntity(new UrlEncodedFormEntity(postDatas, "UTF-8"));
				httpClient = HttpUtil.getNewHttpClient();
				HttpProtocolParams.setUserAgent(httpClient.getParams(), UA_ANDROID);
				HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIME_OUT);
				HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIME_OUT);
				httpResponse = httpClient.execute(httpPost);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					clientStrResult = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
					cookieStore = ((AbstractHttpClient) httpClient).getCookieStore();
				}
			}
			
			//把HttpClient的Cookies转化为Jsoup用的Cookies
			cookies.putAll(HttpUtil.CookieStroeToHashMap(cookieStore));
			
			//获取单点登录地址
			doc = Jsoup.parse(clientStrResult);
			ssoUrl = doc.select("a").first().attr("href");
			//System.out.println(ssoUrl);
			
			//访问单点登录网址获取Cookies
			httpGet = new HttpGet(ssoUrl);
			httpClient = HttpUtil.getNewHttpClient();
			HttpProtocolParams.setUserAgent(httpClient.getParams(), UA_ANDROID);
			HttpConnectionParams.setSoTimeout(httpClient.getParams(), TIME_OUT);
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), TIME_OUT);
			httpResponse = httpClient.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				//取得返回的字符串
				clientStrResult = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
				cookieStore = ((AbstractHttpClient) httpClient).getCookieStore();
			}
			cookies.putAll(HttpUtil.CookieStroeToHashMap(cookieStore));
			//System.out.println(clientStrResult);
			
			
			//先检查今天是否已经签过到
			//false
			//{"time":"1365335551","size":"346","star":"4","sent_weibo":"0","sent_weibo_size":"50","date":"20130407","quota":"35488","uid":"31951221","sina_uid":"2161697961"}
			res = Jsoup.connect(signinfoUrl).cookies(cookies).userAgent(UA_ANDROID).timeout(TIME_OUT).referrer(loginPageUrl).ignoreContentType(true).method(Method.GET).execute();
			cookies.putAll(res.cookies());
			//System.out.println(res.body());
			String signinfoStr = res.body();
			if(signinfoStr.equals("false"))
			{
				//今天还没签过到
				//访问签到链接，并获取签到返回的Json数据
				//{"error_code":"C1","error":"\u5df2\u7ecf\u7b7e\u5230\u8fc7\u4e86"} //今天已签过到
				//[346,4] //获得346M空间，4星级运气
				res = Jsoup.connect(siginUrl).cookies(cookies).userAgent(UA_ANDROID).timeout(TIME_OUT).referrer(ssoUrl).ignoreContentType(true).method(Method.GET).execute();
				cookies.putAll(res.cookies());
				String signReturnStr = res.body();
				if(signReturnStr.contains("error_code\":\"C1"))
				{
					resultFlag = "true";
					resultStr = "今天已签过到";
				}
				else
				{
					String signedSpaceStr = signReturnStr.replace("[", "").replace("]", "").split(",")[0];
					int signedSpace = Integer.valueOf(signedSpaceStr);//获得的容量
					
					//获取配置信息里是否需要转发微博的配置
					boolean isSendWeibo = true;
					try {
						//1=>需要转发，0=>不需要转发
						isSendWeibo = new JSONObject(cfg).getBoolean("sendweibo");
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					if(isSendWeibo == false)
					{
						//用户设置不转发微博
						resultFlag = "true";
						resultStr = "签到成功，根据用户设置不转发微博，共获得" + signedSpace + "M空间";
					}
					else
					{
						//如果发送微博失败，则进行重试
						for(int i=0;i<RETRY_TIMES;i++)
						{
							try {
								//50 //仅返回获得的容量数
								String sendWeiBoMsg = "我刚刚用Android上的《一键签到》应用签到微盘，增加了" + signedSpace + "M空间，这个应用可自动签到贴吧、有信、爱聊、网盘、BBS等多种网站　详细可看：http://oks.shumei.me/supportlist.html　";
								sendWeiBoMsg = URLEncoder.encode(sendWeiBoMsg, "UTF-8");
								doc = Jsoup.connect(sendWeiBoUrl).data("msg", sendWeiBoMsg).cookies(cookies).userAgent(UA_ANDROID).timeout(TIME_OUT).referrer(loginSubmitUrl).ignoreContentType(true).post();
								int weiboAwardSpace = Integer.valueOf(doc.text());
								int totalSpace = signedSpace + weiboAwardSpace;
								
								resultFlag = "true";
								resultStr = "签到获得" + signedSpace + "M空间，转发微博成功获得" + weiboAwardSpace + "M空间，共获得" + totalSpace + "M空间";
								break;//跳出重试循环
							} catch (Exception e) {
								resultStr = "签到成功但转发微博失败，获得" + signedSpace + "M空间";
							}
						}
					}
				}
			}
			else
			{
				//{"time":"1365335551","size":"346","star":"4","sent_weibo":"0","sent_weibo_size":"50","date":"20130407","quota":"35488","uid":"31955221","sina_uid":"2161677961"}
				JSONObject jsonObj = new JSONObject(signinfoStr);
				String sizeStr = jsonObj.getString("size");
				String sent_weibo_sizeStr = jsonObj.getString("sent_weibo_size");
				int size = Integer.valueOf(sizeStr);
				int sent_weibo_size = Integer.valueOf(sent_weibo_sizeStr);
				int totalSize = size + sent_weibo_size;
				resultFlag = "true";
				resultStr = "今天已签过到，共获得" + totalSize + "M空间";
			}
			
			
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
