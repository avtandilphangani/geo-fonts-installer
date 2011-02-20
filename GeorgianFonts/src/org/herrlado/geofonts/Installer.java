package org.herrlado.geofonts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.herrlado.geofonts.ShellCommand.CommandResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class Installer extends Activity implements OnClickListener,
DialogInterface.OnClickListener {

	public static final String TAG = "GeoFontsInstaller";
	public static final String DroidSansBold = "DroidSans-Bold.ttf";
	public static final String DroidSans = "DroidSans.ttf";
	public static final String DroidSansMono = "DroidSansMono.ttf";
	public static final String DroidSerifBold = "DroidSerif-Bold.ttf";
	public static final String DroidSerifBoldItalic = "DroidSerif-BoldItalic.ttf";
	public static final String DroidSerifItalic = "DroidSerif-Italic.ttf";
	public static final String DroidSerifRegular = "DroidSerif-Regular.ttf";
	public static final HashMap<String, String> MD5 = new HashMap<String, String>();
	public static final String DESTINATION = "/system/fonts";
	public static final Pattern MOUNT_SYSTEM_PATTERN = Pattern
	.compile("([^\\s]*)\\s.*(/system)\\s.*");

	static {
		MD5.put(DroidSansBold, "bd3163f7db07b82158a310efd77902ce");
		MD5.put(DroidSans, "741daf0f3e7c8320a752116eefe2f0a0");
		MD5.put(DroidSansMono, "0294cd8fb13d6504622afd3e003033fa");
		MD5.put(DroidSerifBold, "9723725c63b2a2a016ad7831042b4b60");
		MD5.put(DroidSerifBoldItalic, "04154e4911adba25105d3d01276359e0");
		MD5.put(DroidSerifItalic, "8dccfdce11daa12537fde6cd16dc5bf2");
		MD5.put(DroidSerifRegular, "ad860d4a21a857bdee918d902829990a");
	}

	private void alertUser(int id, int icon, DialogInterface.OnClickListener listener, String text){
		new AlertDialog.Builder(this)
		.setMessage(text)
		.setCancelable(false)
		.setTitle(id)
		.setIcon(icon)
		.setPositiveButton(R.string.alert_ok, listener).show();
	}

	private void notifyUser(String message){
		alertUser(R.string.alert_info, android.R.drawable.ic_dialog_info, null, message);
	}

	private String getBackupFolder() {
		return Environment.getExternalStorageDirectory() + "/"
		+ getApplicationContext().getPackageName();
	}

	private String getTmpFontFolder() {
		return getBackupFolder() + "/tmp";
	}

	public boolean restore() {
		StringBuilder sb = new StringBuilder();
		String system = null;
		File backUp = new File(getBackupFolder());
		try{
			ShellCommand sc = new ShellCommand();
			system = getSystemPartion();

			if(!backUp.isDirectory()){
				Log.w(TAG, "backUp is not a directory");
				return false;
			}

			String cc = "mount -o remount, rw" + system + " /system";
			sb.append(cc).append("\n");

			check(sc.su.runWaitFor(cc));

			for(String font : MD5.keySet()){
				String source = getBackupFolder() + "/" + font;
				String dest = DESTINATION + "/" + font;
				cc = "cat " + source + " > " + dest;
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));

				cc = "chmod 644  " + dest;
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));
			}
			try {
				cc = "mount -o remount,ro " + system + " /system";
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));
			} catch (Exception ex) {
				Log.w(TAG, "Cannot mount /system ro :(", ex);
			}
		}catch (Exception e) {
			notifyUser(e.getMessage());
		}
		return true;
	}

	private String getSystemPartion() {

		ShellCommand sc = new ShellCommand();
		CommandResult cr = sc.su.runWaitFor("mount");
		String stdout = cr.stdout;

		try {
			LineNumberReader lineNumberReader = new LineNumberReader(
					new StringReader(stdout));
			String line = null;
			while ((line = lineNumberReader.readLine()) != null) {
				Matcher m = MOUNT_SYSTEM_PATTERN.matcher(line);
				if (m.matches()) {
					return m.group(1);
				}
			}

		} catch (Exception ex) {
			Log.w(TAG, ex.getMessage(), ex);
		}
		return null;
	}

	private static void check(CommandResult runWaitFor) throws Exception {
		if (runWaitFor.success() == false) {
			throw new Exception(runWaitFor.stderr);
		}
	}

	private void uninstallThis() {
		Uri uri = Uri.fromParts("package", getApplication().getPackageName(),
				null);
		Intent deleteThis = new Intent(Intent.ACTION_DELETE, uri);
		startActivity(deleteThis);
	}

	private void getPage(){
		Uri url = Uri.parse("http://www.addictivetips.com/mobile/how-to-root-your-android-phone-device/");
		Intent launchBrowser = new Intent(Intent.ACTION_VIEW, url);
		startActivity(launchBrowser);
	}

	private void reboot(){
		ShellCommand sc = new ShellCommand();
		sc.su.run("su");
		sc.su.run("reboot");
	}

	private Installer inst(){
		return this;
	}

	public boolean backup() {
		File backupFolder = new File(getBackupFolder());
		backupFolder.mkdir();
		if (backupFolder.isDirectory() == false
				|| backupFolder.canWrite() == false) {
			Log.w(TAG,
					backupFolder
					+ " can not be created or it exists and is not a directory or not writable!");
			return false;
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			for (String font : MD5.keySet()) {
				fis = new FileInputStream(new File("/system/fonts/" + font));
				fos = new FileOutputStream(new File(backupFolder + "/" + font));
				IOUtils.copy(fis, fos);
			}
		} catch (Exception ex) {
			notifyUser(ex.getMessage());
			return false;
		} finally {
			IOUtils.closeQuietly(fis);
			IOUtils.closeQuietly(fos);
		}

		Log.d(TAG, "Fonts were copied into the " + backupFolder);
		return true;
	}

	public boolean extractFonts() {
		String folder = getTmpFontFolder() + "/";
		new File(folder).mkdir();
		InputStream is = null;
		OutputStream os = null;
		try {
			for (String key : MD5.keySet()) {
				is = getApplicationContext().getAssets().open(key);
				os = new FileOutputStream(new File(folder + key));
				IOUtils.copy(is, os);
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(os);
			}
		} catch (Exception ex) {
			Log.w(TAG, "Cannot extract Fonts to " + getTmpFontFolder(), ex);
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
			return false;
		}
		return true;
	}

	public boolean installFonts() {
		StringBuilder sb = new StringBuilder();
		String system = null;
		try {
			ShellCommand sc = new ShellCommand();

			if (extractFonts() == false) {
				Log.w(TAG, "Cannot extract Georgian fonts from apk!");
				return false;
			}

			system = getSystemPartion();

			if (system == null) {
				Log.w(TAG,
				"Cannot find out which partion is mounted on /system");
				return false;
			}

			String cc = "mount -o remount, rw" + system + " /system";
			sb.append(cc).append("\n");

			check(sc.su.runWaitFor(cc));

			String sourceFolder = getTmpFontFolder();
			for (String key : MD5.keySet()) {
				String source = sourceFolder + "/" + key;
				String dest = DESTINATION + "/" + key;
				cc = "cat " + source + " > " + dest;
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));

				cc = "chmod 644  " + dest;
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));

				cc = "rm " + source;
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));
			}
			try {
				cc = "mount -o remount,ro " + system + " /system";
				sb.append(cc).append("\n");
				check(sc.su.runWaitFor(cc));
			} catch (Exception ex) {
				Log.w(TAG, "Cannot mount /system ro :(", ex);
			}
			return true;

		} catch (Exception ex) {
			Log.w(TAG, "Cannot install Georgian fonts. Please check the logs",
					ex);
			return false;
		} finally {
			String command = sb.toString();
			Log.i(TAG, "run commands >>>>");
			Log.i(TAG, command);

		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.georgian_installed);

		Button button = (Button) findViewById(R.id.uninstall_this_app);
		button.setOnClickListener(this);

		ShellCommand shc = new ShellCommand();
		if (shc.canSU(true) == false) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.alert_warn)
			.setMessage("This app cannot gain Super User permissions. Is your device rooted?")
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setPositiveButton(R.string.alert_ok, null)
			.setNegativeButton(R.string.alert_help, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int id) {
					getPage();
				}
			}).show();
			//                      alertUser(R.string.alert_warn,
			//                                      "This app cannot gain Super User permissions. Is your device rooted?",
			//                                      android.R.drawable.ic_dialog_alert, null);
			return;
		}

		button = (Button) findViewById(R.id.install_fonts);
		button.setVisibility(View.VISIBLE);
		button.setEnabled(true);
		button.setOnClickListener(this);

		button = (Button) findViewById(R.id.restore_fonts);
		button.setVisibility(View.VISIBLE);
		if(new File(getBackupFolder()).exists()){
			button.setEnabled(true);
		}else{
			button.setEnabled(false);
		}
		button.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.uninstall_this_app) {
			uninstallThis();
		} else if (v.getId() == R.id.install_fonts) {
			alertUser(R.string.alert_warn,
					android.R.drawable.ic_dialog_alert, this,
			"This app now mounts /system partions rw and replaces Droid*.ttf fonts in /system/fonts/");
		}else if(v.getId() == R.id.restore_fonts){
			if(restore()){
				notifyUser("Fonts restored");
			}
		}
	}

	public void enableView() {
		Button button = (Button) findViewById(R.id.uninstall_this_app);
		button.setEnabled(true);
		button = (Button) findViewById(R.id.install_fonts);
		button.setEnabled(true);
		ProgressBar pb = (ProgressBar) findViewById(R.id.installing);
		pb.setVisibility(View.INVISIBLE);
		button = (Button) findViewById(R.id.restore_fonts);
		if(backup()){
			button.setEnabled(true);
		}
	}

	public void disableView() {
		Button button = (Button) findViewById(R.id.uninstall_this_app);
		button.setEnabled(false);
		button = (Button) findViewById(R.id.install_fonts);
		button.setEnabled(false);
		ProgressBar pb = (ProgressBar) findViewById(R.id.installing);
		pb.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(final DialogInterface dialog, int which) {
		if (DialogInterface.BUTTON_POSITIVE == which) {
			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(Void... params) {

					if (backup() == false) {
						Log.w(TAG, "Cannot backup fonts. Please check logs");
						return false;
					}

					return installFonts();
				}

				@Override
				protected void onPreExecute() {
					super.onPreExecute();
					Installer.this.disableView();
				}

				@Override
				protected void onPostExecute(Boolean result) {
					Installer.this.enableView();
					if (result) {
						new AlertDialog.Builder(inst())
						.setTitle("Installation Sucsessful")
						.setIcon(android.R.drawable.ic_dialog_info)
						.setMessage("Reboot the device for changes to take effect! reboot now?")
						.setCancelable(false)
						.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								reboot();
							}
						})
						.setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();                                                                
							}
						}).show();
					} else {
						alertUser(R.string.alert_warn,
								android.R.drawable.ic_dialog_alert, null,
						"The fonts were not installed. Please check the logs and contact the developer :(");
					}
					super.onPostExecute(result);
				}
			}.execute();
		}
	}

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override

	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.send_logs: // start settings activity
			de.ub0r.android.lib.Log.collectAndSendLog(this);
			return true;
		default:
			return false;
		}
	}

}