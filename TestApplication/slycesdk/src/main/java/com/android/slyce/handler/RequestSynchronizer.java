package com.android.slyce.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.MoodStocksProgress;
import com.android.slyce.models.SlyceProgress;
import com.android.slyce.utils.SlyceLog;

import org.json.JSONArray;

public class RequestSynchronizer extends Handler {

    private final String TAG = RequestSynchronizer.class.getSimpleName();

    private OnSlyceRequestListener mRequestListener;

    public RequestSynchronizer(OnSlyceRequestListener listener){
        super(Looper.getMainLooper());
        mRequestListener = listener;
    }

    public void onSlyceProgress(long progress, String message, String token){
        SlyceProgress slyceProgress = new SlyceProgress(progress, message, token);
        obtainMessage(1, slyceProgress).sendToTarget();
    }

    public void on2DRecognition(String irId, String productInfo){
        MoodStocksProgress moodStocksProgress = new MoodStocksProgress(irId, productInfo);
        obtainMessage(2, moodStocksProgress).sendToTarget();
    }

    public void on3DRecognition(JSONArray products){
        obtainMessage(3, products).sendToTarget();
    }

    public void onError(String message){
        obtainMessage(4, message).sendToTarget();
    }

    public void onStageLevelFinish(OnSlyceRequestListener.StageMessage message){
        obtainMessage(5, message).sendToTarget();
    }

    public void on2DExtendedRecognition(JSONArray products){
        obtainMessage(6, products).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){

            case 1:

                SlyceProgress slyceProgress = (SlyceProgress) msg.obj;

                long progress = slyceProgress.progress;
                String progressMsg = slyceProgress.message;
                String token = slyceProgress.token;

                mRequestListener.onSlyceProgress(progress, progressMsg, token);

                SlyceLog.i(TAG, "onSlyceProgress(" + progress + ", " + progressMsg + ", " + token + ")");

                break;

            case 2:

                MoodStocksProgress moodStocksProgress = (MoodStocksProgress) msg.obj;

                String irid = moodStocksProgress.irId;
                String productInfo = moodStocksProgress.productInfo;

                mRequestListener.on2DRecognition(irid, productInfo);

                SlyceLog.i(TAG, "on2DRecognition(" + irid + ", " + productInfo + ")");

                break;

            case 3:

                JSONArray products = (JSONArray) msg.obj;

                mRequestListener.on3DRecognition(products);

                SlyceLog.i(TAG, "on3DRecognition(" + products + ")");

                break;

            case 4:

                String message = (String) msg.obj;

                mRequestListener.onError(message);

                SlyceLog.i(TAG, "onError(" + message + ")");

                break;

            case 5:

                OnSlyceRequestListener.StageMessage stageMsg = (OnSlyceRequestListener.StageMessage) msg.obj;

                mRequestListener.onStageLevelFinish(stageMsg);

                SlyceLog.i(TAG, "onStageLevelFinish(" + stageMsg + ")");

                break;

            case 6:

                JSONArray extenedInfo = (JSONArray) msg.obj;

                mRequestListener.on2DExtendedRecognition(extenedInfo);

                SlyceLog.i(TAG, "on2DExtendedRecognition(" + extenedInfo + ")");

                break;
        }
    }
}
