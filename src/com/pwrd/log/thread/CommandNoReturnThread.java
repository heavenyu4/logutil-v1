package com.pwrd.log.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pwrd.log.callback.NotifyCallBack;
import com.pwrd.log.callback.RefreshUICallBack;
import com.pwrd.log.utils.OperLog;
import com.pwrd.log.utils.StringUtils;
import com.sun.media.jfxmediaimpl.platform.Platform;

import sun.java2d.loops.ProcessPath.ProcessHandler;

public class CommandNoReturnThread extends Thread {

	private StringBuffer sb;
	private String command;
	private RefreshUICallBack callBack;
	private NotifyCallBack mNotifyCallBack;

	public CommandNoReturnThread(String commandStr, RefreshUICallBack refreshUICallBack) {
		this(commandStr, refreshUICallBack, null);
	}

	public CommandNoReturnThread(String commandStr, RefreshUICallBack refreshUICallBack,
			NotifyCallBack notifyCallBack) {
		sb = new StringBuffer();
		command = "cmd /k " + commandStr;
		callBack = refreshUICallBack;
		mNotifyCallBack = notifyCallBack;
	}

	@Override
	public void run() {
		BufferedReader br = null;
		try {
			String operation = "exec:\t" + command;
			OperLog.getInstance().recordLog(operation);
			Process p = Runtime.getRuntime().exec(command);

			InputStream errorStream = p.getErrorStream();

			BufferedReader errbufferedReader = new BufferedReader(new InputStreamReader(errorStream, "GBK"));

			InputStream is = p.getInputStream() != null ? p.getInputStream() : errorStream;
//			br = new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
			br = new BufferedReader(new InputStreamReader(is, "GBK"));
			String line = null;

			if (mNotifyCallBack != null) {
				List<String> searchPids = search();
				String operation1 = "search result: " + searchPids;
				OperLog.getInstance().recordLog(operation1);
				String pid = searchPids.get(searchPids.size() - 1);
				mNotifyCallBack.action(pid);
			}

			if (is != errorStream) {
				String errLine = null;
				while (!StringUtils.isEmpty(errLine = errbufferedReader.readLine())) {
					OperLog.getInstance().recordLog(errLine);
//					callBack.refreshUI(line);
				}
			}
			while ((line = br.readLine()) != null) {
				if (!StringUtils.isEmpty(line)) {
					sb.append(line + "\n");
					OperLog.getInstance().recordLog(line);
					callBack.refreshUI(line);
				}
			}

//			if (is != null) {
////				sb.append("The command has been executed!\n");
//				callBack.refreshUI(sb.toString());
//			}
			OperLog.getInstance().recordLog(sb.toString());

		} catch (Exception e) {
			e.printStackTrace();
			OperLog.getInstance().recordLog(e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 查找adb.exe的所有进程号
	 * 
	 * @return
	 */
	public List<String> search() {
		String cmd = "tasklist /fi \"imagename eq adb.exe\" /fo list";

		BufferedReader bufferedReader = null;
		List<String> list = new ArrayList<String>();

		try {
			Process process = Runtime.getRuntime().exec(cmd);
			OperLog.getInstance().recordLog(cmd);

			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str;
			while ((str = bufferedReader.readLine()) != null) {

				if (str.startsWith("PID:")) {
					String[] array = str.split(":");
					String pid = array[1].trim();

					list.add(pid);
				}

			}

		} catch (IOException e) {
			OperLog.getInstance().recordLog(e.getMessage());
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				e.getMessage();
			}
		}
		OperLog.getInstance().recordLog(list.toString());
		return list;
	}

}
