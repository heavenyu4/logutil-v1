package com.pwrd.log.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.pwrd.log.bean.CmdInfo;
import com.pwrd.log.bean.DeviceBean;

/**
 * 设备信息管理
 * @author Administrator
 *
 */
public class DeviceDataManager {

	private DeviceDataManager() {
	}

	private static volatile DeviceDataManager instance = null;

	public static DeviceDataManager getInstance() {
		if (instance == null) {
			synchronized (DeviceDataManager.class) {
				if (instance == null) {
					instance = new DeviceDataManager();
				}
			}
		}
		return instance;
	}

	//设备列表
	private CopyOnWriteArrayList<DeviceBean> deviceList = new CopyOnWriteArrayList<DeviceBean>();
	// 与上次刷新相比, 下线的设备列表
	private CopyOnWriteArrayList<DeviceBean> deviceOfflineList;

	public List<DeviceBean> getDeviceList() {
		return deviceList;
	}

	/**
	 * 设置新的设备列表, 刷新后重新设置设备列表用
	 * @param deviceList
	 */
	public void setDeviceList(CopyOnWriteArrayList<DeviceBean> deviceList) {
		OperLog.getInstance().recordLog("setDeviceList: " + deviceList);
		// 更新下命令参数, 传入的deviceList参数只有设备信息, 命令信息是空的
		if (deviceList != null && deviceList.size() > 0) {
			for (int i = 0; i < deviceList.size(); i++) {
				DeviceBean bean = deviceList.get(i);
				bean.cmdInfo = findDeviceCmd(bean);
			}
			if (this.deviceList != null && this.deviceList.size() > 0) {
				deviceOfflineList = new CopyOnWriteArrayList<DeviceBean>();
				for (int i = 0; i < this.deviceList.size(); i++) {
					DeviceBean bean1 = this.deviceList.get(i);
//						for (int j = 0; j < deviceList.size();j++) {
//							if (bean1.serialNo.equals(deviceList.get(j).serialNo)) {
//								break;
//							}
//						}
					if (!deviceList.contains(bean1)) {
						deviceOfflineList.add(bean1);
					}
				}
			}

		}

		this.deviceList = deviceList;
	}

	/**
	 * 获取断开连接的设备列表
	 * @return
	 */
	public synchronized CopyOnWriteArrayList<DeviceBean> getDeviceOfflineList() {
		return deviceOfflineList;
	}

	/**
	 * 设置命令行信息
	 * @param in
	 */
	public void setCmd(DeviceBean in) {
		if (in == null) {
			OperLog.getInstance().recordLog("setCmd: in null");
			return;
		}
		if (deviceList == null) {
			OperLog.getInstance().recordLog("setCmd: deviceList null");
			return;
		}

		for (int i = 0; i < deviceList.size(); i++) {
			DeviceBean deviceBean = deviceList.get(i);
			if (deviceBean.serialNo.equals(in.serialNo)) {
				deviceBean.cmdInfo = in.cmdInfo;
				break;
			}
		}
	}

	/**
	 * 查找设备抓取log命令的进程号pid
	 * @param in
	 * @return
	 */
	public String findDevicePid(DeviceBean in) {
		if (in == null) {
			OperLog.getInstance().recordLog("findDevicePid: in null");
			return null;
		}
		if (deviceList == null) {
			OperLog.getInstance().recordLog("findDevicePid: deviceList null");
			return null;
		}
		for (int i = 0; i < deviceList.size(); i++) {
			DeviceBean deviceBean = deviceList.get(i);
			if (deviceBean.serialNo.equals(in.serialNo)) {
				if (deviceBean.cmdInfo != null) {
					return deviceBean.cmdInfo.pid;
				} else {
					return null;
				}
			}
		}
		return null;

	}

	/**
	 * 移除设备的命令信息, 停止抓log后的操作
	 * @param in
	 */
	public void removeDeviceCmd(DeviceBean in) {
		if (in == null) {
			OperLog.getInstance().recordLog("removeDeviceCmd: in null");
			return;
		}
		if (deviceList == null) {
			OperLog.getInstance().recordLog("removeDeviceCmd: deviceList null");
			return;
		}
		for (int i = 0; i < deviceList.size(); i++) {
			DeviceBean deviceBean = deviceList.get(i);
			if (deviceBean.serialNo.equals(in.serialNo)) {
				deviceBean.cmdInfo = null;
				break;
			}
		}
		return;
	}

	/**
	 * 查找设备的命令信息
	 * @param in
	 * @return
	 */
	public CmdInfo findDeviceCmd(DeviceBean in) {
		if (in == null) {
			OperLog.getInstance().recordLog("findDeviceCmd: in null");
			return null;
		}
		if (deviceList == null) {
			OperLog.getInstance().recordLog("findDeviceCmd: deviceList null");
			return null;
		}
		for (int i = 0; i < deviceList.size(); i++) {
			DeviceBean deviceBean = deviceList.get(i);
			if (deviceBean.serialNo.equals(in.serialNo)) {
				return deviceBean.cmdInfo;
			}
		}
		return null;

	}

	/**
	 * 获取所有正在抓log的设备信息, 退出时杀进程用
	 * @return
	 */
	public List<DeviceBean> getLogProcess() {
		ArrayList<DeviceBean> list = new ArrayList<DeviceBean>();
		if (deviceList == null) {
			OperLog.getInstance().recordLog("getLogProcess: deviceList null");
			return null;
		}
		for (int i = 0; i < deviceList.size(); i++) {
			DeviceBean deviceBean = deviceList.get(i);
			if (deviceBean.cmdInfo != null) {
				list.add(deviceBean);
			}
		}
		return list;
	}

}
