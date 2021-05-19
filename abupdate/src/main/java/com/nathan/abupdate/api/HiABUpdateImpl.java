package com.nathan.abupdate.api;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UpdateEngine;
import android.util.Log;

import androidx.annotation.IntDef;


import com.nathan.abupdate.UpdateConfig;
import com.nathan.abupdate.UpdateManager;
import com.nathan.abupdate.UpdaterState;
import com.nathan.abupdate.util.FileDownloader;
import com.nathan.abupdate.util.UpdateConfigs;
import com.nathan.abupdate.util.UpdateEngineErrorCodes;
import com.nathan.abupdate.util.UpdateEngineStatuses;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Paths;

import static com.nathan.abupdate.util.PackageFiles.OTA_PACKAGE_DIR;


public class HiABUpdateImpl implements HiABUpdate {

    private static final String TAG = "HiABUpdateImpl";
    private static boolean INIT = false;
    private UpdateManager mUpdateManager = null;

    private Handler mHandler = null;
    private HandlerThread handlerThread = null;

    @IntDef(value = {
            UpdateAction.UPDATE_ACTION_STOP, UpdateAction.UPDATE_ACTION_RESET,
            UpdateAction.UPDATE_ACTION_SUSPEND, UpdateAction.UPDATE_ACTION_RESUME })
    @Retention(RetentionPolicy.CLASS)
    public @interface UpdateAction {
        int UPDATE_ACTION_STOP = 0;
        int UPDATE_ACTION_RESET = 1;
        int UPDATE_ACTION_SUSPEND = 2;
        int UPDATE_ACTION_RESUME = 3;
    }
    private Context mContext;
//    private Handler.Callback mCallback = new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message message) {
//
//            return false;
//        }
//    };

    private static final String PREFIX_FILE = "file://";
    private static final String PREFIX_ONLINE_CLEAR = "http://";
    private static final String PREFIX_ONLINE = "https://";
    private static final String SUFFIX_ONLINE = ".json";
    private static final String JSON_STRING = "{";

    private UpdateCallback mUpdateCallback;
    public interface UpdateCallback{
        /**
         * @param state {@link UpdaterState}
         */
        void onUpdaterStateChange(int state);

        /**
         * @param status {@link UpdateEngine.UpdateStatusConstants}
         */
        void onEngineStatusUpdate(String status);

        /**
         * @param errorCode {@link UpdateEngine.ErrorCodeConstants}
         */
        void onEnginePayloadApplicationComplete(String errorCode);

        void onProgressUpdate(int progress);
    }

    @Override
    public void setUpdateCallbackListener(UpdateCallback callback){
        this.mUpdateCallback = callback;
    }

    @Override
    public void init(){
        handlerThread = new HandlerThread("abupdate");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mUpdateManager = new UpdateManager(new UpdateEngine(), mHandler);
        this.mUpdateManager.setOnStateChangeCallback(this::onUpdaterStateChange);
        this.mUpdateManager.setOnEngineStatusUpdateCallback(this::onEngineStatusUpdate);
        this.mUpdateManager.setOnEngineCompleteCallback(this::onEnginePayloadApplicationComplete);
        this.mUpdateManager.setOnProgressUpdateCallback(this::onProgressUpdate);
        this.mUpdateManager.bind();
        INIT = true;
    }

    @Override
    public void applyUpdateConfig(final String url,Context context) {
        Log.d(TAG, "applyUpdateConfig: getUrl:"+url);
        if (!url.startsWith(PREFIX_FILE) && !url.startsWith(PREFIX_ONLINE)
                && !url.startsWith(PREFIX_ONLINE_CLEAR) && !url.startsWith(JSON_STRING)) {
            throw  new TypeNotPresentException(url, new Throwable("url must be started with " +
                    " one of (1)file://  (2)http://  (3)https:// (4){"));
        }
        if (!INIT) {
            mContext = context.getApplicationContext();
            throw new NoSuchMethodError("You must call init() function firstly");
        }

        if (url.startsWith(PREFIX_FILE)) {
           UpdateConfig updateConfig =  UpdateConfigs.getUpdateConfigsFromLocal(url.substring(7));
           if (updateConfig == null) {
               throw new NullPointerException("can not find any .json to parse!");
           }
            applyUpdate(updateConfig,context);
        } else if (url.startsWith(PREFIX_ONLINE) || url.startsWith(PREFIX_ONLINE_CLEAR)) {
            //TODO 1 download
            if (mHandler == null) {
                throw new NullPointerException("handler must not be null!");
            }
            mHandler.post(() ->{
                String localPath = OTA_PACKAGE_DIR + url.substring(url.lastIndexOf("/"));
                Log.d(TAG, url+" to localPath "+localPath);
                try {
                    FileDownloader downloader = new FileDownloader(
                            url,0,0,
                            Paths.get(localPath).toFile());
                    downloader.downloadFile();
                    //TODO 2 applyUpdate
                    UpdateConfig updateConfig =  UpdateConfigs.getUpdateConfigsFromLocal(localPath);
                    if (updateConfig == null) {
                        throw new NullPointerException("can not find any .json to parse!");
                    }
                    applyUpdate(updateConfig,context);
                }catch (IOException e){
                    e.printStackTrace();
                }
            });
        } else if (url.startsWith(JSON_STRING)) {
            //TODO 1 download
            if (mHandler == null) {
                throw new NullPointerException("handler must not be null!");
            }
            mHandler.post(() ->{
                //TODO 2 applyUpdate
                UpdateConfig updateConfig =  UpdateConfigs.getUpdateConfigsFromString(url);
                if (updateConfig == null) {
                    throw new NullPointerException("can not find any .json to parse!");
                }
                applyUpdate(updateConfig,context);
            });
        }


    }

    @Override
    public void sendUpdateAction(@UpdateAction int action) {
        if (action == UpdateAction.UPDATE_ACTION_RESET) {
            resetUpdate();
        } else if (action == UpdateAction.UPDATE_ACTION_STOP) {
            cancelRunningUpdate();
        } else if (action == UpdateAction.UPDATE_ACTION_SUSPEND) {
            onSuspendClick();
        } else if (action == UpdateAction.UPDATE_ACTION_RESUME) {
            onResumeClick();
        }
    }

    /**
     * switch slot button clicked
     */
    @Override
    public void switchSlot() {
        mUpdateManager.setSwitchSlotOnReboot();
    }

    @Override
    public void destroy() {
        this.mUpdateManager.unbind();
        this.mUpdateManager.setOnEngineStatusUpdateCallback(null);
        this.mUpdateManager.setOnProgressUpdateCallback(null);
        this.mUpdateManager.setOnEngineCompleteCallback(null);
        handlerThread.quitSafely();
        mUpdateCallback = null;
        mContext = null;
        mUpdateManager = null;
        mHandler = null;
    }

    private void cancelRunningUpdate() {
        try {
            mUpdateManager.cancelRunningUpdate();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to cancel running update", e);
        }
    }

    private void resetUpdate() {
        try {
            mUpdateManager.resetUpdate();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to reset update", e);
        }
    }

    private void applyUpdate(UpdateConfig config, Context context) {
        try {
            mUpdateManager.applyUpdateApi(context.getApplicationContext(), config);
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to apply update " + config.getName(), e);
        }
    }

    /**
     * suspend button clicked
     */
    private void onSuspendClick() {
        try {
            mUpdateManager.suspend();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to suspend running update", e);
        }
    }

    /**
     * resume button clicked
     */
    private void onResumeClick() {
        try {
            mUpdateManager.resume();
        } catch (UpdaterState.InvalidTransitionException e) {
            Log.e(TAG, "Failed to resume running update", e);
        }
    }


    /**
     * Invoked when SystemUpdaterSample app state changes.
     * Value of {@code state} will be one of the
     * values from {@link UpdaterState}.
     */
    private void onUpdaterStateChange(int state) {
        Log.i(TAG, "UpdaterStateChange state="
                + UpdaterState.getStateText(state)
                + "/" + state);
        if (mUpdateCallback == null) return;
        mUpdateCallback.onUpdaterStateChange(state);
    }

    /**
     * Invoked when {@link UpdateEngine} status changes. Value of {@code status} will
     * be one of the values from {@link UpdateEngine.UpdateStatusConstants}.
     */
    private void onEngineStatusUpdate(int status) {
        Log.i(TAG, "StatusUpdate - status="
                + UpdateEngineStatuses.getStatusText(status)
                + "/" + status);
        String statusText = UpdateEngineStatuses.getStatusText(status);
        if (mUpdateCallback == null) return;
        mUpdateCallback.onEngineStatusUpdate(statusText + "/" + status);
    }

    /**
     * Invoked when the payload has been applied, whether successfully or
     * unsuccessfully. The value of {@code errorCode} will be one of the
     * values from {@link UpdateEngine.ErrorCodeConstants}.
     */
    private void onEnginePayloadApplicationComplete(int errorCode) {
        final String completionState = UpdateEngineErrorCodes.isUpdateSucceeded(errorCode)
                ? "SUCCESS"
                : "FAILURE";
        Log.i(TAG,
                "PayloadApplicationCompleted - errorCode="
                        + UpdateEngineErrorCodes.getCodeName(errorCode) + "/" + errorCode
                        + " " + completionState);
        String errorText = UpdateEngineErrorCodes.getCodeName(errorCode);
        if (mUpdateCallback == null) return;
        mUpdateCallback.onEnginePayloadApplicationComplete(errorText + "/" + errorCode);
    }

    /**
     * Invoked when update progress changes.
     */
    private void onProgressUpdate(double progress) {
        if (mUpdateCallback == null) return;
        mUpdateCallback.onProgressUpdate((int) (100 * progress));
    }


}
