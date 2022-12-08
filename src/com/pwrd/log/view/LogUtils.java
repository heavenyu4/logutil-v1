package com.pwrd.log.view;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import com.google.gson.Gson;
import com.pwrd.log.bean.CmdInfo;
import com.pwrd.log.bean.ConfigBean;
import com.pwrd.log.bean.DeviceBean;
import com.pwrd.log.callback.DeviceChangeListener;
import com.pwrd.log.callback.DeviceClickListener;
import com.pwrd.log.callback.NotifyCallBack;
import com.pwrd.log.callback.RefreshUICallBack;
import com.pwrd.log.thread.CommandNoReturnThread;
import com.pwrd.log.thread.CommandThread;
import com.pwrd.log.utils.DeviceDataManager;
import com.pwrd.log.utils.OperLog;
import com.pwrd.log.utils.StringUtils;

/**
 * 抓取log主方法
 * 
 * @author Administrator
 *
 */
public class LogUtils {

	private StringBuilder sb;
	private final String TIP_ADVANCED = "如果您想使用高级版的话, 请在点击开始后, 重启游戏, 再执行您的游戏操作~";
	private final String TIP_SELECT_DEVICE = "请从设备列表中选择要抓取log的设备~";
	Font fontContent, fontTitle;
//	ArrayList<String> cmdList;
	JButton btnStart;
	JLabel deviceLabel;
	JTextArea tipArea;

	// 当前选中的设备
	DeviceBean currentDeviceBean = null;
	private static String strPath = "";
	private JTextArea logArea;
	/**
	 * 停止或是窗口关闭时, 是否需要删除onesdk的调试文件 我们测试不需要一直删这个文件
	 */
	private boolean delDebugFileSwitch = true;
	public static String libDir = "";
	private static String logDir = "";

	public static void main(String[] args) {
		System.out.println();
		if (args != null && args.length == 2) {
			libDir = args[0];
			logDir = args[1];
			
		} else {
			try {
				strPath = URLDecoder.decode(System.getProperty("user.dir"), "utf-8");
				libDir = strPath + "\\library";
				logDir = strPath + "\\log";
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		OperLog.getInstance().setPath(libDir);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LogUtils window = new LogUtils();
				} catch (Exception e) {
					e.printStackTrace();
					OperLog.getInstance().recordLog(e.getMessage());
				}
			}
		});

	}

	public LogUtils() {
		init();
	}

	private void init() {
		sb = new StringBuilder();
		// 字体设置
		fontTitle = new Font("微软雅黑", Font.BOLD, 14);
		fontContent = new Font("微软雅黑", Font.PLAIN, 12);

//		cmdList = new ArrayList<String>();

		JFrame frm = new JFrame();
		frm.setTitle("抓Log工具");
		int xOffset = 300;
		int width = 500;
		frm.setBounds(xOffset, 100, 900, 620);
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.getContentPane().setLayout(null);
		frm.setResizable(false);
		// 设置windows界面效果
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		frm.setVisible(true);
		frm.setIconImage(new ImageIcon(libDir + "\\bugfly.jpg").getImage());

		// 去除空格后的路径名
		String subPath = strPath.replace(" ", "");
		if ((subPath.length() < strPath.length()) || strPath.contains("(") || strPath.contains(")")) {
			String pathErr = "path err: [" + strPath + "]";
			OperLog.getInstance().recordLog(pathErr);
			JOptionPane.showMessageDialog(frm, "log工具所在路径非法! \n请放置到无空格无()的路径下", "警告", JOptionPane.ERROR_MESSAGE);
		}
		OperLog.getInstance().recordLog(strPath);

		deviceLabel = new JLabel();
		deviceLabel.setBounds(xOffset, 20, width, 30);
		deviceLabel.setFont(fontTitle);

		frm.add(deviceLabel);

		tipArea = new JTextArea();
		tipArea.setBounds(xOffset, deviceLabel.getY() + deviceLabel.getHeight() + 10, width, 50);
		tipArea.setText(TIP_SELECT_DEVICE);
		tipArea.setLineWrap(true);
		tipArea.setFont(fontContent);
		frm.add(tipArea);

		// log文件名
		JPanel logNamePal = new JPanel();
		logNamePal.setLayout(new BoxLayout(logNamePal, BoxLayout.X_AXIS));
		logNamePal.setBounds(xOffset, tipArea.getY() + tipArea.getHeight() + 10, width, 30);

		JLabel logNameLabel = new JLabel("自定义log文件名:");
		logNameLabel.setFont(fontTitle);
		logNamePal.add(logNameLabel);
//		logNameLabel.setSize(60, 50);

		logNamePal.add(Box.createHorizontalStrut(10));

		JTextField logNameText = new JTextField("");
		logNameText.setFont(fontContent);
		logNameText.setToolTipText("请输入log文件名, 您不输入的话, 以当前时间为log文件名");
		logNameText.setFont(fontContent);
		logNamePal.add(logNameText);

		frm.add(logNamePal);

		GridLayout layout = new GridLayout(1, 3);
		layout.setHgap(10);
		layout.setVgap(10);
		JPanel btnPanel = new JPanel(layout);
		btnPanel.setBounds(xOffset, logNamePal.getY() + logNamePal.getHeight() + 10, width, 50);
		frm.add(btnPanel);

		btnStart = new JButton("开始");
//		btnStart.setBounds(xOffset, 100, 100, 50);
		btnStart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentDeviceBean == null) {
					JOptionPane.showMessageDialog(frm, "请选择设备!", "提示", JOptionPane.WARNING_MESSAGE);
					return;
				}

				String lognameInput = logNameText.getText().replace(" ", "");
				String logName = "";
				if (currentDeviceBean != null) {
					if (currentDeviceBean.manufacturer.contains(currentDeviceBean.model)) {
						logName += currentDeviceBean.manufacturer.replace(" ", "") + "_";

					} else {
						logName += currentDeviceBean.manufacturer.replace(" ", "") + "_"
								+ currentDeviceBean.model.replace(" ", "") + "_";
					}
				}
				if (StringUtils.isEmpty(lognameInput)) {
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
					String format = sdf.format(date);
					logName += format + ".log";
				} else {
					if (!isFileNameValid(lognameInput)) {
						JOptionPane.showMessageDialog(frm, "log文件名不合法, 不能包含" + "< > / \\ | : * ? aux", "警告",
								JOptionPane.ERROR_MESSAGE);
						return;
					} else {
						logName += lognameInput + ".log";
					}
				}

				btnStart.setEnabled(false);

				// 拷贝onesdk的debug开关文件
				copyDebugFile();
				
				String cmd = libDir + "\\adb.exe -s " + currentDeviceBean.serialNo + " logcat -v threadtime >" + logDir
						+ "\\" + logName;
				updateLogArea("【" + currentDeviceBean.model + "】" + " 开始抓取log: " + logName + " .......");
				try {
					new CommandNoReturnThread(cmd, new RefreshUICallBack() {

						@Override
						public void refreshUI(String string) {

						}
					}, new NotifyCallBack() {

						@Override
						public void action(String pid) {
							// TODO Auto-generated method stub
							String operation = "pid: " + pid;
							OperLog.getInstance().recordLog(operation);

							if (currentDeviceBean != null) {
								currentDeviceBean.cmdInfo = new CmdInfo(cmd, pid);
								DeviceDataManager.getInstance().setCmd(currentDeviceBean);
							}

						}
					}).start();
				} catch (Exception e1) {
					OperLog.getInstance().recordLog(e1.getMessage());
				}
			}

		});
		btnStart.setFont(fontTitle);
		btnPanel.add(btnStart);

		JButton btnEnd = new JButton("停止");
//		btnEnd.setBounds(xOffset+200, 100, 100, 50);
		btnEnd.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btnStart.setEnabled(true);
				delDebugFile();

				if (currentDeviceBean != null) {
					String pid = DeviceDataManager.getInstance().findDevicePid(currentDeviceBean);
					if (!StringUtils.isEmpty(pid)) {
						String cmd = " taskkill /f /pid " + pid;
						new CommandThread(cmd, new RefreshUICallBack() {

							@Override
							public void refreshUI(String string) {
								DeviceDataManager.getInstance().removeDeviceCmd(currentDeviceBean);
							}
						}).start();

						updateLogArea("【" + currentDeviceBean.model + "】 抓取log停止");
					}

				}

			}
		});
		btnEnd.setFont(fontTitle);
		btnPanel.add(btnEnd);

		JButton btnOpenDir = new JButton();
		btnOpenDir.setText("打开日志目录");
		btnOpenDir.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
//				String dirPath = System.getProperty("user.dir") + "//log";
				try {
					java.awt.Desktop.getDesktop().open(new File(logDir));
				} catch (IOException e3) {
					e3.printStackTrace();
				}

			}
		});
		btnOpenDir.setFont(fontTitle);
		btnPanel.add(btnOpenDir);

		// 操作记录
		JPanel groupBox = new JPanel();
		logArea = new JTextArea();
//		logArea.setBounds(5, 5, 290, 290);
		logArea.setLineWrap(true);
		logArea.setRows(15);
		logArea.setColumns(40);
		logArea.setFont(fontContent);
		JScrollPane scrollPane = new JScrollPane(logArea);

		// 分别设置水平和垂直滚动条自动出现
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		groupBox.add(scrollPane);
		groupBox.setBorder(BorderFactory.createTitledBorder(null, "操作：", TitledBorder.LEADING,
				TitledBorder.DEFAULT_POSITION, fontTitle));
		groupBox.setBounds(xOffset, btnPanel.getY() + btnPanel.getHeight() + 10, width, 300);

		frm.add(groupBox);

		// 设备列表
		DeviceList deviceList = new DeviceList();
		deviceList.setBounds(10, 50, 250, 370);
		deviceList.setDeviceClickListener(new DeviceClickListener() {

			@Override
			public void onClick(DeviceBean deviceInfo) {
				tipArea.setText(TIP_ADVANCED);
//				btnEnd.doClick();
				deviceLabel.setText("机型: " + deviceInfo);
				logNameText.setText("");
				currentDeviceBean = deviceInfo;
				String pid = DeviceDataManager.getInstance().findDevicePid(currentDeviceBean);
				if (StringUtils.isEmpty(pid)) {
					btnStart.setEnabled(true);
				} else {
					// 之前在抓着log
					btnStart.setEnabled(false);
				}
			}
		});
		deviceList.setDeviceChangeListener(new DeviceChangeListener() {

			@Override
			public void onDevicesChanged() {
				refreshCmd();
			}
		});
		frm.add(deviceList);

		frm.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {

			}

			@Override
			public void windowClosing(WindowEvent e) {
				// 窗口关闭时, 停止抓log
				// 这时候需要把onesdk开关的debug文件删下, 避免影响游戏测试的效果
				delDebugFile();
				List<DeviceBean> list = DeviceDataManager.getInstance().getLogProcess();
				if (list != null && list.size() > 0) {
					for (int i = 0; i < list.size(); i++) {
						OperLog.getInstance().recordLog("exit del process: " + list.get(i).cmdInfo);
						String pid = list.get(i).cmdInfo.pid;
						if (!StringUtils.isEmpty(pid)) {
							String cmd = " taskkill /f /pid " + pid;
							new CommandThread(cmd, new RefreshUICallBack() {

								@Override
								public void refreshUI(String string) {
//									DeviceDataManager.getInstance().removeDeviceCmd(currentDeviceBean);
								}
							}).start();

//							updateLogArea(currentDeviceBean.model + " 抓取log停止");
						}
					}
				}

			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		delDebugFileSwitch = checkDebugFileSwitch();
	}

	protected void refreshCmd() {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				btnStart.setEnabled(true);
				currentDeviceBean = null;
				deviceLabel.setText("机型: ");
				tipArea.setText(TIP_SELECT_DEVICE);

				CopyOnWriteArrayList<DeviceBean> deviceOfflineList = DeviceDataManager.getInstance()
						.getDeviceOfflineList();
				if (deviceOfflineList != null && deviceOfflineList.size() > 0) {
					for (int i = 0; i < deviceOfflineList.size(); i++) {
						if (deviceOfflineList.get(i).cmdInfo != null) {
							updateLogArea("【" + deviceOfflineList.get(i).model + "】" + " 抓取log停止");
						}
					}

				}
			}
		});
//		}

	}

	// 拷贝onesdk的debug开关文件
	private void copyDebugFile() {
		String cmd = libDir + "//adb.exe -s " + currentDeviceBean.serialNo + " push " + libDir
				+ "//onesdk_develop.properties /sdcard/onesdk_develop.properties";
		new CommandThread(cmd, new RefreshUICallBack() {

			@Override
			public void refreshUI(String string) {
			}
		}).start();
		
	}

	// 删除onesdk的debug开关文件
	private void delDebugFile() {
		if (currentDeviceBean == null) {
			return;
		}
		if (!delDebugFileSwitch) {
			return;
		}
		String cmd = libDir + "//adb.exe -s " + currentDeviceBean.serialNo
				+ " shell rm /sdcard/onesdk_develop.properties";
		new CommandThread(cmd, new RefreshUICallBack() {

			@Override
			public void refreshUI(String string) {
			}
		}).start();
		
		
	}

	private boolean isFileNameValid(String fileName) {
		String[] notValidStr = { "<", ">", "|", ":", "*", "?", "/", "\\" };
		for (int i = 0; i < notValidStr.length; i++) {
			if (fileName.contains(notValidStr[i])) {
				return false;
			}
		}

		if (fileName.equals("aux")) {
			return false;
		}
		return true;
	}

	private void updateLogArea(String text) {
		sb.append(text).append("\n");
		logArea.setText(sb.toString());
	}

//	private String findDeviceInCmd(String serialNo) {
//		String cmd = "";
//		if (cmdList.size() > 0) {
//			for (int i = 0; i < cmdList.size(); i++) {
//				if (cmdList.get(i).contains(serialNo)) {
//					cmd = cmdList.get(i);
//					break;
//				}
//			}
//		}
//		return cmd;
//	}

	private boolean checkDebugFileSwitch() {
		boolean delDebugFile = true;
		File file = new File(libDir + "//config.json");
		if (file.exists()) {
			try {
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line = null;
				StringBuilder sb = new StringBuilder();
//				while((line=bufferedReader.readLine())!=null) {
				while (!StringUtils.isEmpty(line = bufferedReader.readLine())) {
					sb.append(line);
				}
				OperLog.getInstance().recordLog(sb.toString());
				Gson gson = new Gson();
				ConfigBean configBean = gson.fromJson(sb.toString(), ConfigBean.class);
				delDebugFile = configBean.delDebugFile;

			} catch (Exception e) {
				e.printStackTrace();
				OperLog.getInstance().recordLog(e.getStackTrace().toString());
			}
		}

		OperLog.getInstance().recordLog("delDebugFile:" + delDebugFile);
		return delDebugFile;

	}

}
